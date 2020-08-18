/*
 * Copyright (c) 2016 - now David Sehnal, licensed under Apache 2.0, See LICENSE file for more info.
 */

namespace LiteMol.Visualization.Molecule.PaperChain {
    "use strict";

    const enum Constants {
        MetalDashSize = 0.15
    }
    
    import BT = Core.Structure.BondType
    import TableImpl = Core.Utils.DataTable;
    import Atom = LiteMol.Core.Structure.Atom;
    import Geom = Core.Geometry
    import Vec3 = Geom.LinearAlgebra.Vector3
    import Mat4 = Geom.LinearAlgebra.Matrix4
    import GB = Geometry.Builder

    function isHydrogen(n: string) {
        return n === 'H' || n === 'D' || n === 'T';
    }

    function getAtomCount(model: Core.Structure.Molecule.Model, atomIndices: number[], params: Parameters) {
        const { elementSymbol:es } = model.data.atoms;
        const { hideHydrogens } = params;
        
        let atomCount = 0;

        if (hideHydrogens) {
            for (const aI of atomIndices) {
                if (!isHydrogen(es[aI])) {
                    atomCount++;
                }
            }
        }
        else
            {
            atomCount = atomIndices.length;
        }

        return atomCount;
    }

    function getBondsInfo(model: Core.Structure.Molecule.Model, atomIndices: number[], params: Parameters) {            
        const bonds = Core.Structure.computeBonds(model, atomIndices, {
            maxHbondLength:params.customMaxBondLengths && params.customMaxBondLengths.has('H') ? params.customMaxBondLengths.get('H')! : 1.15
        });

        const metalDashFactor = 1 / (2 * Constants.MetalDashSize);
        const { elementSymbol:es } = model.data.atoms;
        const { hideHydrogens } = params;

        let covalentStickCount = 0, dashPartCount = 0;
        let { type, count, atomAIndex, atomBIndex } = bonds;
        let { x, y, z } = model.positions;
        for (let i = 0; i < count; i++) {
            const t = type[i];
            const a = atomAIndex[i], b = atomBIndex[i];

            if (hideHydrogens && (isHydrogen(es[a]) || isHydrogen(es[b]))) {
                continue;
            }

            if (t === BT.Unknown || t === BT.DisulfideBridge) covalentStickCount += 1;
            else if (t >= BT.Single && t <= BT.Aromatic) covalentStickCount += t;
            else if (t === BT.Metallic || t === BT.Ion || t === BT.Hydrogen) {
                const dx = x[a] - x[b], dy = y[a] - y[b], dz = z[a] - z[b];
                const len = Math.sqrt(dx*dx + dy*dy + dz*dz);
                dashPartCount += Math.ceil(metalDashFactor * len);
            }
        }

        return {
            bonds,
            covalentStickCount,
            dashPartCount
        }
    }

    class BondModelState {
        bondUpVector = Vec3.fromValues(1, 0, 0);
        dir = Vec3.zero();

        scale = Vec3.zero();
        rotation = Mat4.zero();
        
        offset =  Vec3.zero();
        a = Vec3.zero();
        b = Vec3.zero();

        constructor(
            public bondTemplate: Geometry.RawGeometry,
            public builder: GB) {
        }

    }

    namespace Templates {
        const bondCache: {[t: number]: Geometry.RawGeometry} = {};
        export function getBond(tessalation: number) {
            if (bondCache[tessalation]) return bondCache[tessalation];
            let detail: number;

            switch (tessalation) {
                case 0: detail = 2; break;
                case 1: detail = 4; break;
                case 2: detail = 6; break;
                case 3: detail = 8; break;
                case 4: detail = 10; break;
                case 5: detail = 12; break;
                default: detail = 14; break;
            }

            const geom = new THREE.TubeGeometry(new THREE.LineCurve3(new THREE.Vector3(0, 0, 0), new THREE.Vector3(1, 0, 0)) as any, 2, 1.0, detail);
            const ret = GeometryHelper.toRawGeometry(geom);
            bondCache[tessalation] = ret;
            return ret;
        }

        const atomCache: {[t: number]:  Geometry.RawGeometry} = {};
        export function getAtom(tessalation: number) {
            if (atomCache[tessalation]) return atomCache[tessalation];
            let base: any, radius = 1;
            switch (tessalation) {
                case 0: base = new THREE.OctahedronGeometry(radius, 0); break;
                case 1: base = new THREE.OctahedronGeometry(radius, 1); break;
                case 2: base = new THREE.IcosahedronGeometry(radius, 0); break;
                case 3: base = new THREE.IcosahedronGeometry(radius, 1); break;
                case 4: base = new THREE.IcosahedronGeometry(radius, 2); break;
                case 5: base = new THREE.OctahedronGeometry(radius, 3); break;
                default: base = new THREE.IcosahedronGeometry(radius, 3); break;
            }
            const ret = GeometryHelper.toRawGeometry(base);
            atomCache[tessalation] = ret;
            return ret;
        }
    }

    class BuildState {
        tessalation = this.params.tessalation;
        atomRadius = this.params.atomRadius;
        bondRadius = this.params.bondRadius;
        hideBonds = this.params.hideBonds;

        bondTemplate = Templates.getBond(this.tessalation!);
        atomTemplate = Templates.getAtom(this.tessalation!);
        dashTemplate = GB.getDashTemplate();

        atomCount = getAtomCount(this.model, this.atomIndices, this.params);
        atomVertexCount = this.atomTemplate.vertexCount * this.atomCount;
        atomBuilder = GB.createStatic(this.atomVertexCount, this.atomTemplate.indexCount * this.atomCount);
        atomColors = new Float32Array(this.atomVertexCount * 3);
        atomPickColors = new Float32Array(this.atomVertexCount * 4);
        smallRingList:SmallRing[];
        smallRingLinkages:SmallRingLinkages;
        molAtoms:MolAtom[];

        atoms = this.model.data.atoms;
        positions = this.model.positions;
        cX = this.positions.x; cY = this.positions.y; cZ = this.positions.z;
        atomSymbols = this.atoms.elementSymbol;
        residueIndex = this.atoms.residueIndex;

        scale = Vec3.zero();
        translation = Vec3.zero();
        
        pickColor = { r: 0.1, g: 0.1, b: 0.1 };
        pickOffset = 0;

        atomMapBuilder = new Selection.VertexMapBuilder(this.atomIndices.length);
        
        bs: BondsBuildState = <any>void 0;
        currentMaxPathLength: number;
        currentMaxRingSize: number;

        constructor(public model: Core.Structure.Molecule.Model, public atomIndices: number[], public params: Parameters) {

        }
    }

    class BondsBuildState {
        info = getBondsInfo(this.state.model, this.state.atomIndices, this.state.params);

        bondVertexCount = this.state.bondTemplate.vertexCount * this.info.covalentStickCount + this.state.dashTemplate.vertexCount * this.info.dashPartCount;
        bondBuilder = GB.createStatic(this.bondVertexCount, this.state.bondTemplate.indexCount * this.info.covalentStickCount + this.state.dashTemplate.indexCount * this.info.dashPartCount);
        bondColors = new Float32Array(this.bondVertexCount * 3);
        
        bondRadius = this.state.params.bondRadius;
        bondState = new BondModelState(this.state.bondTemplate, this.bondBuilder);
        bondCount = this.info.bonds.count;

        constructor(public state: BuildState) {
        }
    }

    class PaperChainGeometryBuilder {
        static VMD_PI = 3.14159265358979323846;
        atomsGeometry: THREE.BufferGeometry;
        bondsGeometry: THREE.BufferGeometry;

        pickGeometry: THREE.BufferGeometry;

        atomVertexMap: Selection.VertexMap;
        bondVertexMap: Selection.VertexMap;

        vertexStateBuffer: THREE.BufferAttribute;

        dispose() {
            this.atomsGeometry.dispose();
            this.bondsGeometry.dispose();
            this.pickGeometry.dispose();
        }

        private static addAtom(a: number, state: BuildState)
        {
            state.atomMapBuilder.startElement(a);

            let r = state.atomRadius!(a);

            Vec3.set(state.scale, r, r, r);
            Vec3.set(state.translation, state.cX[a], state.cY[a], state.cZ[a]);

            const startVertexOffset = state.atomBuilder.vertices.elementCount; //!!!.vertexOffset;
            GB.addRawTransformed(state.atomBuilder, state.atomTemplate, state.scale, state.translation, void 0);

            Selection.Picking.assignPickColor(a, state.pickColor);

            for (let i = 0, _b = state.atomTemplate.vertexCount; i < _b; i++) {
                state.atomPickColors[state.pickOffset++] = state.pickColor.r;
                state.atomPickColors[state.pickOffset++] = state.pickColor.g;
                state.atomPickColors[state.pickOffset++] = state.pickColor.b;
                state.pickOffset++; // 4th component
            }

            state.atomMapBuilder.addVertexRange(startVertexOffset, state.atomBuilder.vertices.elementCount /*!!!.vertexOffset*/);
            state.atomMapBuilder.endElement();
            let molAtom:MolAtom = new MolAtom();
            //molAtom.init();//Start by adding MolAtoms
        }
        
        private static addBond(b: number, state: BuildState, bs: BondsBuildState) {
            let aI = bs.info.bonds.atomAIndex[b], bI = bs.info.bonds.atomBIndex[b], type = bs.info.bonds.type[b];

            if (state.params.hideHydrogens) {
                const { elementSymbol: es } = state.model.data.atoms;
                if (isHydrogen(es[aI]) || isHydrogen(es[bI])) {
                    return;
                }
            }

            Vec3.set(bs.bondState.a, state.cX[aI], state.cY[aI], state.cZ[aI]);
            Vec3.set(bs.bondState.b, state.cX[bI], state.cY[bI], state.cZ[bI]);

            const r = +bs.bondRadius!, 
                o = 2 * r / 3,
                h = r / 2;

            const bondState = bs.bondState;
            switch (type) {
                case BT.Unknown:
                case BT.Single:
                case BT.DisulfideBridge:
                    PaperChainGeometryBuilder.addBondPart(r, 0, 0, bondState);
                    break;
                case BT.Double:
                    PaperChainGeometryBuilder.addBondPart(h, o, o, bondState);
                    PaperChainGeometryBuilder.addBondPart(h, -o, -o, bondState);
                    break;
                    case BT.Triple:
                    PaperChainGeometryBuilder.addBondPart(h, 0, o, bondState);
                    const c = Math.cos(Math.PI / 6) * o, s = Math.sin(Math.PI / 6) * o
                    PaperChainGeometryBuilder.addBondPart(h, -c,  -s,  bondState);
                    PaperChainGeometryBuilder.addBondPart(h, c, -s, bondState);
                    break;
                case BT.Aromatic:
                    PaperChainGeometryBuilder.addBondPart(h / 2, o, o, bondState);
                    PaperChainGeometryBuilder.addBondPart(h / 2, -o, -o, bondState);
                    PaperChainGeometryBuilder.addBondPart(h / 2, -o, o, bondState);
                    PaperChainGeometryBuilder.addBondPart(h / 2, o, -o, bondState);
                    break;
                case BT.Metallic:
                case BT.Ion:
                case BT.Hydrogen:
                    PaperChainGeometryBuilder.addDashedBond(h, bondState);
                    break;
            }
        }

        private static addBondPart(r: number, oX: number, oY: number, state: BondModelState) {
            const dir = Vec3.sub(state.dir, state.b, state.a);
            const length = Vec3.magnitude(state.dir);
            
            Vec3.set(state.scale, length, r, r);
            Vec3.makeRotation(state.rotation, state.bondUpVector, dir);
            
            state.offset[0] = 0;
            state.offset[1] = oX;
            state.offset[2] = oY;
            Vec3.transformMat4(state.offset, state.offset, state.rotation);
            Vec3.add(state.offset, state.offset, state.a);

            GB.addRawTransformed(state.builder, state.bondTemplate, state.scale, state.offset, state.rotation);
        }

        private static addDashedBond(r: number, state: BondModelState) {
            GB.addDashedLine(state.builder, state.a, state.b, Constants.MetalDashSize, Constants.MetalDashSize, r);    
        }

        private static getEmptyBondsGeometry() {
            let bondsGeometry = new THREE.BufferGeometry();
            bondsGeometry.addAttribute('position', new THREE.BufferAttribute(new Float32Array(0), 3));
            bondsGeometry.addAttribute('normal', new THREE.BufferAttribute(new Float32Array(0), 3));
            bondsGeometry.addAttribute('index', new THREE.BufferAttribute(new Uint32Array(0), 1));
            bondsGeometry.addAttribute('color', new THREE.BufferAttribute(new Float32Array(0), 3));
            return bondsGeometry;
        }

        private static getBondsGeometry(state: BondsBuildState) {
            const geom = GB.toBufferGeometry(state.bondBuilder);
            Geometry.addAttribute(geom, 'color', state.bondColors, 3);
            return geom;
        }

        private static getAtomsGeometry(state: BuildState)
        {
            const atomsGeometry = GB.toBufferGeometry(state.atomBuilder);
            Geometry.addAttribute(atomsGeometry, 'color', state.atomColors, 3);

            const stateBuffer = new Float32Array(state.atomVertexCount);
            const vertexStateBuffer = new THREE.BufferAttribute(stateBuffer, 1);
            atomsGeometry.addAttribute('vState', vertexStateBuffer);

            const atomsPickGeometry = new THREE.BufferGeometry();
            atomsPickGeometry.addAttribute('position', atomsGeometry.getAttribute('position'));
            atomsPickGeometry.addAttribute('index', atomsGeometry.getAttribute('index'));
            atomsPickGeometry.addAttribute('pColor', new THREE.BufferAttribute(state.atomPickColors, 4));
            
            return {
                vertexStateBuffer,
                atomsGeometry,
                atomsPickGeometry,
                atomVertexMap: state.atomMapBuilder.getMap()
            };
        }

        private static async addAtoms(state: BuildState, ctx: Core.Computation.Context) {
            const chunkSize = 2500;
            let started = Core.Utils.PerformanceMonitor.currentTime();

            const { elementSymbol } = state.model.data.atoms;
            const { hideHydrogens } = state.params;

            for (let start = 0, _l = state.atomIndices.length; start < _l; start += chunkSize) {
                for (let i = start, _b = Math.min(start + chunkSize, state.atomIndices.length); i < _b; i++) {
                    const aI = state.atomIndices[i];
                    if (hideHydrogens && isHydrogen(elementSymbol[aI])) {
                        continue;
                    }
                    PaperChainGeometryBuilder.addAtom(aI, state);
                }
                let t = Core.Utils.PerformanceMonitor.currentTime();
                if (t - started > Core.Computation.UpdateProgressDelta) {
                    started = t;
                    await ctx.updateProgress('Adding atoms...', true, start, _l);
                }
            }
        }

        private static async addBondsChunks(state: BuildState, bs: BondsBuildState, ctx: Core.Computation.Context) {
            const chunkSize = 2500;
            let started = Core.Utils.PerformanceMonitor.currentTime();

            for (let start = 0; start < bs.bondCount; start += chunkSize) {                
                for (let i = start, _b = Math.min(start + chunkSize, bs.bondCount); i < _b; i++) {
                    PaperChainGeometryBuilder.addBond(i, state, bs);
                }
                let t = Core.Utils.PerformanceMonitor.currentTime();
                if (t - started > Core.Computation.UpdateProgressDelta) {
                    started = t;
                    await ctx.updateProgress('Adding bonds...', true, start, bs.bondCount);
                }
            }
        }

        private static async addBonds(state: BuildState, ctx: Core.Computation.Context) {
            if (state.params.hideBonds) {
                return;
            }

            await ctx.updateProgress('Computing bonds...', true);

            let bs = new BondsBuildState(state);
            state.bs = bs;
            await PaperChainGeometryBuilder.addBondsChunks(state, bs, ctx);
        }

        private static async addRings(state: BuildState)
        {

        }


        private static find_small_rings_and_links(state: BuildState, maxpathlength:number, maxringsize:number)
        {
            // skip ring finding if we've already done it
            if (maxpathlength == state.currentMaxPathLength && maxringsize == state.currentMaxRingSize)
                return;
            state.currentMaxPathLength = maxpathlength;
            state.currentMaxRingSize = maxringsize;

            // Clear smallringList, smallringLinkages
            state.smallRingList = [];
            state.smallRingLinkages = new SmallRingLinkages();

            // find groups of atoms bonded into small rings
            this.find_small_rings(state, maxringsize);

            // orientate the rings if possible
            this.orientate_small_rings(state, maxringsize);

            let nOrientatedRings = 0;
            for (let i=0; i < state.smallRingList.length; i++)
            {
                if (state.smallRingList[i].orientated)
                    nOrientatedRings++;
            }
            // find paths between rings
            this.find_orientated_small_ring_linkages(state, maxpathlength, maxringsize);
        }

         // find all loops less than a given size
        private static find_small_rings(state: BuildState, maxringsize:number)
        {
            let n_back_edges:number, n_rings:number;
            let back_edge_src:number[] = [];
            let back_edge_dest:number[] = [];

            n_back_edges = this.find_back_edges(state, back_edge_src, back_edge_dest);
            n_rings = this.find_small_rings_from_back_edges(state, maxringsize, back_edge_src, back_edge_dest);
            return n_rings; // number of rings found
        }

         // find the back edges of an arbitrary spanning tree (or set of trees if the molecule is disconnected)
        private static find_back_edges(state: BuildState, back_edge_src:number[], back_edge_dest:number[])
        {
            let n_back_edges = 0;
            let nAtoms = state.atomIndices.length;
            let intree_parents:number[] = this.emptyArrayCustom(nAtoms, -1);// clear parents, -1 = not in tree, -2 = no parent

            for (let i=0; i<nAtoms; i++)
            {
                if (intree_parents[i] == -1)
                {  // not been visited
                    n_back_edges += this.find_connected_subgraph_back_edges(state, i,back_edge_src,back_edge_dest,intree_parents);
                }
            }

            return n_back_edges;
        }


        // find the back edges of a spanning tree (for a connected portion of the molecule)
        private static find_connected_subgraph_back_edges(state: BuildState, atomid:number, back_edge_src:number[], back_edge_dest:number[], intree_parents:number[])
        {
            let i, n_new_back_edges, cur_atom_id:number, child_atom_id, parent_atom_id;
            let node_stack:number[] = [atomid];

            intree_parents[atomid] = -2; // in tree, but doesn't have parent
            n_new_back_edges = 0;

            while (node_stack.length > 0)
            {
                // @ts-ignore
                cur_atom_id = node_stack.pop();

                let curatom:Atom = state.atoms.getRow(cur_atom_id);
                parent_atom_id = intree_parents[cur_atom_id];

                for(i = 0; i < curatom.bonds; i++)
                {
                       child_atom_id = curatom.bondTo[i];
                       if (intree_parents[child_atom_id] != -1)
                       {
                            // back-edge found
                            if ((child_atom_id != parent_atom_id) && (child_atom_id > cur_atom_id))
                            {
                                     // we ignore edges back to the parent
                                     // and only add each back edge once
                                     // (it'll crop up twice since each bond is listed on both atoms)
                                     back_edge_src.push(cur_atom_id);
                                     back_edge_dest.push(child_atom_id);
                                     n_new_back_edges++;
                            }
                       }
                       else
                           {
                                // extended tree
                                intree_parents[child_atom_id] = cur_atom_id;
                                node_stack.push(child_atom_id);
                          }
                     }
               }

       return n_new_back_edges;
     }

 // find rings smaller than maxringsize given list of back edges
 private static find_small_rings_from_back_edges(state: BuildState, maxringsize:number, back_edge_src:number[], back_edge_dest:number[])
    {
        let i:number, key:number, max_rings:number;
        let n_rings = 0;
        let n_back_edges = back_edge_src.length;
        let ring:SmallRing;
        let used_edges: { [id: number] : number; } = {};//new inthash_t; // back edges which have been dealt with
        let used_atoms: { [id: number] : number; }= {};//new inthash_t; // atoms (other than the first) which are used in the current path (i.e. possible loop)
        let nAtoms = state.atomIndices.length;
         // cap the peak number of rings to find based on the size of the
         // input structure.  This should help prevent unusual structures
         // with very high connectivity, such as silicon nanodevices from
         // blowing up the ring search code
        max_rings = 2000 + (100.0*Math.sqrt(nAtoms));

        for(i = 0;i<n_back_edges;i++)
        {
            ring = new SmallRing();
            ring.append(back_edge_src[i]);
            ring.append(back_edge_dest[i]);

            // first atom is not marked used, since we're allowed to re-use it.
            used_atoms[back_edge_dest[i]] = 1;

            n_rings += this.find_small_rings_from_partial(state, ring, maxringsize, used_edges, used_atoms);

              // abort if there are too many rings
            if (n_rings > max_rings)
            {
                console.log( "Maximum number of rings ("  + max_rings  + ") exceed."
                        + " Stopped looking for rings after "  + n_rings  + " rings found.");
                break;
            }

            key = this.get_edge_key(back_edge_src[i],back_edge_dest[i]);
            used_edges[key] = 1;

            // remove last atom from used_atoms
            delete used_atoms[back_edge_dest[i]];
         }

         return n_rings;
     }

    // find rings smaller than maxringsize from the given partial ring (don't reuse used_edges)
    private static find_small_rings_from_partial(state: BuildState, ring:SmallRing, maxringsize:number, used_edges: { [id: number]: number }, used_atoms:{ [id: number]: number })
    {
        let i:number, next_bond_pos:number, child_atom_id:number, bond_key:number, barred:number, closes:number, do_pop:number;
        let cur_atom_id:number;
        let n_rings = 0;
        let curatom;

        let atom_id_stack:number[] = [];//Max ring sizes
        let bond_pos_stack:number[] = []//maxringsize;
        atom_id_stack.push(ring.last_atom());
        bond_pos_stack.push(0);
        // @ts-ignore
        cur_atom_id = atom_id_stack.pop();

        while (cur_atom_id != -1)
        {
            // @ts-ignore
            next_bond_pos = bond_pos_stack.pop();
            curatom = atom(cur_atom_id);
            do_pop = 0; // whether we need to pop the current atom

            // if the ring is already maxringsize, just pop the current atom off.
            if (ring.num() > maxringsize)
                do_pop = 1;

            if (next_bond_pos == 0 && !do_pop)
            {
                // ignore barred rings (by checking for links back to earlier parts of the ring
                // before exploring further)
                barred = closes = 0;
                for(i=0;i<curatom.bonds;i++)
                {
                    child_atom_id = curatom.bondTo[i];

                    // check that this is not an edge immediately back to the previous atom
                    if (child_atom_id == ring.atoms[ring.atoms.length-2]) continue;

                    // check that this is not an atom we've included
                    // (an exception is the first atom, which we're allowed to try add, obviously :)
                    if (used_atoms[child_atom_id] != undefined)
                    {
                        barred = 1;
                        continue;
                    }

                    // check is this not a back edge which has already been used
                    bond_key = this.get_edge_key(state, cur_atom_id,child_atom_id);
                    if (used_edges[bond_key] != undefined)
                    {
                        if (child_atom_id == ring.first_atom())
                        barred = 1; // used back-edge which closes the ring counts as barred
                        continue;
                    }

                    // check whether ring closes
                    if (child_atom_id == ring.first_atom())
                    {
                        closes = 1;
                        continue;
                    }
                }

                if (closes && !barred)
                {
                    state.smallRingList.push(ring.copy());
                    n_rings += 1;
                }
                if (closes || barred)
                {
                    // adding this atom would create a barred ring, so skip to next atom on stack
                    do_pop = 1;
                }
            }

            if (!do_pop)
            {
                for(i=next_bond_pos;i<curatom.bonds;i++)
                {
                    child_atom_id = curatom.bondTo[i];

                    // check that this is not an edge immediately back to the previous atom
                    if (child_atom_id == ring.atoms[ring.atoms.length-2]) continue;

                    // check is this not a back edge which has already been used
                    bond_key = this.get_edge_key(state, cur_atom_id,child_atom_id);
                    if (used_edges[bond_key] != undefined) continue;

                    // Append child atom and go deeper
                    ring.append(child_atom_id);
                    used_atoms[child_atom_id] = 1;

                    // push current state and new state onto stack and recurse
                    atom_id_stack.push(cur_atom_id);
                    bond_pos_stack.push(i+1);
                    atom_id_stack.push(child_atom_id);
                    bond_pos_stack.push(0);
                    break;
                }

                // we finished the children, pop one parent
                if(i>=curatom.bonds)
                    do_pop = 1;
            }

            if (do_pop && atom_id_stack.length > 0)
            {
                // finished with cur_atom_id and all its children
                // clean up before returning from recurse
                ring.remove_last();
                delete used_atoms[cur_atom_id];
            }
        }


        return n_rings;
    }


    // construct edge key
    private static get_edge_key(state:BuildState, edge_src:number, edge_dest:number)
    {
        let t:number;
        if (edge_dest > edge_src) {
            t = edge_src;
            edge_src = edge_dest;
            edge_dest = t;
        }
        return edge_src * state.atomIndices.length + edge_dest;
    }

    // Routines for orientating rings
    // contributed by Simon Cross and Michelle Kuttel

    private static orientate_small_rings(state:BuildState, maxringsize:number)
    {
        let smallRingList:SmallRing[] = state.smallRingList;
        for (let i = 0; i<smallRingList.length; i++)
            this.orientate_small_ring(state, smallRingList[i], maxringsize);
    }

    private static orientate_small_ring(state:BuildState, ring:SmallRing , maxringsize:number)
    {
        let oxygen:number = -1;
        let oxygen_typeindex:number, c1_nameindex:number, c1p_nameindex:number, cu1_nameindex:number;
        let c2_nameindex:number, c2p_nameindex:number, cu2_nameindex:number;
        let curatom:Atom, atom_before_O:Atom, atom_after_O:Atom;
        let atomTable: TableImpl<Atom> = state.atoms;
        // lookup atom name indices in advance
        oxygen_typeindex = atomTypes.typecode("O");
        c1_nameindex = atomNames.typecode("C1");
        c1p_nameindex = atomNames.typecode("C1'");
        cu1_nameindex = atomNames.typecode("C_1");
        c2_nameindex = atomNames.typecode("C2");
        c2p_nameindex = atomNames.typecode("C2'");
        cu2_nameindex = atomNames.typecode("C_2");

        // Find an oxygen (or something with two bonds)
        for (let i = 0; i < ring.num(); i++)
        {
            curatom = atomTable.getRow(ring.get(i));//atom(ring[i]);
            if (curatom.atomicnumber == 8 || curatom.typeindex == oxygen_typeindex || curatom.bonds == 2)
            {
                oxygen = i;
                break;
            }
        }

        // leave ring unorientated if no oxygen found
        if (oxygen == -1) return;

        // find atoms before and after oxygen (taking into account wrapping)
        if (oxygen == 0)
            atom_before_O = atom(ring.last_atom());
        else
            atom_before_O = atom(ring[oxygen-1]);

        if (oxygen == ring.num()-1)
            atom_after_O = atom(ring.first_atom());
        else
            atom_after_O = atom(ring[oxygen+1]);

        // ensure C1 carbon is after the oxygen
        // leave unorientated if the C1 carbon can't be found
        if (atom_before_O.nameindex == c1_nameindex || atom_before_O.nameindex == c1p_nameindex
        || atom_before_O.nameindex == cu1_nameindex
        || atom_before_O.nameindex == c2_nameindex || atom_before_O.nameindex == c2p_nameindex
        || atom_before_O.nameindex == cu2_nameindex)
        {
            ring.reverse();
            ring.orientated = 1;
        }
        else if (atom_after_O.nameindex == c1_nameindex || atom_after_O.nameindex == c1p_nameindex
        || atom_after_O.nameindex == cu1_nameindex
        || atom_after_O.nameindex == c2_nameindex || atom_after_O.nameindex == c2p_nameindex
        || atom_after_O.nameindex == cu2_nameindex)
        {
            ring.orientated = 1;
        }
    }


    // Routines for finding links between rings
    // contributed by Simon Cross and Michelle Kuttel

    // find all paths shorter than a given length between the small rings
    // listed in smallringList
    private static find_orientated_small_ring_linkages(state:BuildState, maxpathlength:number, maxringsize:number)
    {
        let sr:SmallRing;
        let lp:LinkagePath;
        let i:number, j:number, atom_id:number;
        let n_paths:number = 0;
        let atom_to_ring:{[id:number]:number} = [];
        let multi_ring_atoms:{[id:number]:number} = [];
        let used_atoms:{[id:number]:number} = [];

        // create lookup from atom id to ring id
        for (i = 0; i < state.smallRingList.length; i++)
        {
            sr = state.smallRingList[i];
            // XXX: Uncomment this if we want to include non-orientated rings
            //      in linkage paths
            // if (!sr.orientated) continue;
            for (j=0;j<sr.num();j++) 
            {
                atom_id = sr.get(j);
                if (atom_to_ring[atom_id] == undefined)
                    atom_to_ring[atom_id] = i;
                else
                    multi_ring_atoms[atom_id] = 1;
            }
        }

        // look for ring linkage paths starting from each atom
        // of each orientated ring
        for (i = 0; i<state.smallRingList.length; i++)
        {
            sr = state.smallRingList[i];
            if (!sr.orientated) continue;
            for (j=0; j<sr.num(); j++)
            {
                if (multi_ring_atoms[sr.get(j)] != undefined) continue;
                lp = new LinkagePath();
                lp.start_ring = i;
                lp.path.append(sr.get(j));
                n_paths += this.find_linkages_for_ring_from_partial(state, lp, maxpathlength, atom_to_ring, multi_ring_atoms, used_atoms);
            }
        }

        return n_paths; // number of paths found.
    }

    private static find_linkages_for_ring_from_partial(state:BuildState, lp:LinkagePath, maxpathlength:number, atom_to_ring:{[id: number]: number}, multi_ring_atoms:{[id: number]: number}, used_atoms:{[id: number]: number})
    {
        let i, cur_atom_id:number, next_bond_pos, child_atom_id, ringidx;
        let n_paths = 0;
        let curatom:MolAtom;
    
        let atom_id_stack:number[] = [];
        let bond_pos_stack:number[] = [];
        atom_id_stack.push(lp.path.last_atom());
        bond_pos_stack.push(0);
    
        while (atom_id_stack.length > 0)
        {
            // @ts-ignore
            cur_atom_id = atom_id_stack.pop();
            next_bond_pos = bond_pos_stack.pop();
            curatom = atom(cur_atom_id);
    
            if (next_bond_pos == 0)
                used_atoms[cur_atom_id] = 1;
    
            for(i=next_bond_pos; i < curatom.bonds; i++)
            {
                child_atom_id = curatom.bondTo[i];
    
                // check that this isn't an atom that belongs to multiple rings
                if (multi_ring_atoms[child_atom_id] != undefined) continue;
    
                // check that this is not an edge immediately back to the previous atom
                // (when there is only one atom in the path, it can't be a link back)
                if (lp.num() > 1 && child_atom_id == lp.get(lp.num()-2)) continue;
    
                // check that we haven't arrived at a non-orientated ring
                ringidx = atom_to_ring[child_atom_id];
                if (ringidx != undefined && !state.smallRingList[ringidx].orientated) continue;
    
                // only store paths from smaller ringidx to larger ringidx (to avoid getting a copy of each orientation of the path)
                // ignore paths which return to the same ring
                // check that we're leaving the starting ring
                if (ringidx != undefined && ringidx <= lp.start_ring) continue;
    
                // see if this takes us to another ring
                if (ringidx != undefined && (ringidx > lp.start_ring)) 
                {
                    lp.append(child_atom_id);
                    lp.end_ring = ringidx;
                    state.smallRingLinkages.addLinkagePath(lp.copy());
                    n_paths++;
                    lp.end_ring = -1;
                    lp.remove_last();
                    continue;
                }
    
                // check that this is not an atom we've included
                // (an exception is the first atom, which we're allowed to try add, obviously :)
                if (used_atoms[child_atom_id] != undefined) continue;
    
                if (lp.num() < maxpathlength)
                {
                    lp.append(child_atom_id);
    
                    // push current state and new state onto stack and recurse
                    atom_id_stack.push(cur_atom_id);
                    bond_pos_stack.push(i + 1);
                    atom_id_stack.push(child_atom_id);
                    bond_pos_stack.push(0);
                    break;
                }
            }
    
            if ((i >= curatom.bonds)&&(atom_id_stack.length > 0))
            {
                // clean up before returning from recurse
                lp.remove_last();
                delete used_atoms[cur_atom_id];
            }
        }
    
        return n_paths;
    }

static async build(model: Core.Structure.Molecule.Model,
            parameters: Parameters,
            atomIndices: number[],
            ctx: Core.Computation.Context) {

            await ctx.updateProgress('Creating atoms...');
            let state = new BuildState(model, atomIndices, <Parameters>Core.Utils.extend({}, parameters, DefaultPaperChainModelParameters));
            let atoms = model.positions;
            console.log(atoms.getRow(0));
            await PaperChainGeometryBuilder.addAtoms(state, ctx);
            await PaperChainGeometryBuilder.addBonds(state, ctx);

            await ctx.updateProgress('Finalizing...');

            let ret = new PaperChainGeometry();
            if (state.bs)
            {
                ret.bondsGeometry = PaperChainGeometryBuilder.getBondsGeometry(state.bs);
            }
            else
            {
                ret.bondsGeometry = PaperChainGeometryBuilder.getEmptyBondsGeometry();
            }

            let atomGeometry = PaperChainGeometryBuilder.getAtomsGeometry(state);

            ret.vertexStateBuffer = atomGeometry.vertexStateBuffer;
            ret.atomsGeometry = atomGeometry.atomsGeometry;
            ret.pickGeometry = atomGeometry.atomsPickGeometry;
            ret.atomVertexMap = atomGeometry.atomVertexMap;

            return ret;
        }

        private static sixRingPosition(angles: number[])
        {
            let sixRings:number[][] = [
                [0.0, 0.0, 0.0],          //Planar
                [-35.26, -35.26, -35.26],	//Chairs
                [35.26, 35.26, 35.26],
                [-35.26, 74.20, -35.26],	//Boats
                [35.26, -74.20, 35.26],
                [74.20, -35.26, -35.26],
                [-74.20, 35.26, 35.26],
                [-35.26, -35.26, 74.20],
                [35.26, 35.26, -74.20],
                [-42.16, 9.07, -17.83],   //Hal-Chairs
                [42.16, -9.07, 17.83],
                [42.16, 17.83, -9.06],
                [-42.16, -17.83, 9.06],
                [-17.83, -42.16, 9.07],
                [17.83, 42.16, -9.07],
                [-9.07, 42.16, 17.83],
                [9.07, -42.16, -17.83],
                [9.07, -17.83, -42.16],
                [-9.07, 17.83, 42.16],
                [17.83, -9.07, 42.16],
                [-17.83, 9.07, -42.16],
                [0.0, 50.84, -50.84],    //Skew-Boats
                [0.0, -50.48, 50.48],
                [50.84, -50.84, 0.0],
                [-50.48, 50.48, 0.0],
                [-50.48, 0.0, 50.48],
                [50.48, 0.0, -50.48],
                [-35.26, 17.37, -35.26], //Envelopes
                [35.26, -17.37, 35.26],
                [46.86, 0.0, 0.0],
                [-46.86, 0.0, 0.0],
                [-35.26, -35.26, 17.37],
                [35.26, 35.26, -17.37],
                [0.0, 46.86, 0.0],
                [0.0, -46.86, 0.0],
                [17.37, -35.26, -35.26],
                [-17.37, 35.26, 35.26],
                [0.0, 0.0, 46.86],
                [0.0, 0.0, -46.86] ];

            let pos = -1;
            let range = 15.0;

            //Find the best matching conformation using a sum of squared differences between the angles
            let sum = 100000.0;
            for (let i = 0; i<=38; i++)
            {
                let temp:number = Math.pow(angles[0] - sixRings[i][0], 2) + Math.pow(angles[1] - sixRings[i][1], 2) + Math.pow(angles[2] - sixRings[i][2], 2);
                if(temp < sum) {
                    sum = temp;
                    pos = i;
                }
            }

            //Check if it is a tight match to the ideal conformation
            if(!((sixRings[pos][0]-range) < angles[0] && angles[0] < (sixRings[pos][0]+range) &&
                (sixRings[pos][1]-range) < angles[1] && angles[1] < (sixRings[pos][1]+range) &&
                (sixRings[pos][2]-range) < angles[2] && angles[2] < (sixRings[pos][2]+range)))
            {
                pos *= -1;
            }

            return pos;
        }

        //Returns the position in the array of 5ring conformations to which the Hill Reilly angles correspond
        private static fiveRingPosition(angles:number[])
        {
            let fiveRings:number[][] = [
            [0.0, 0.0],      //Planar
            [-13.10, -33.89],//Twists
            [-42.16, 13.21],
            [34.50, -34.50],
            [-13.21, 42.16],
            [33.89, 13.11],
            [13.10, 33.89],
            [42.16, -13.21],
            [-34.50, 34.50],
            [13.21, -42.16],
            [-33.89, -13.11],
            [-24.88, 40.00],  //Envelopes
            [0.00, -39.90],
            [24.50, 24.50],
            [-39.50, 0.00],
            [39.50, -24.90],
            [24.88, -40.00],
            [0.00, 39.90],
            [-24.50, -24.50],
            [39.50, 0.00],
            [-39.50, 24.90]];

            let pos = -1;
            let range = 10.0;

            //Find the best matching conformation using a sum of squared differences between the angles
            let sum:number = 100000.0;
            for (let i = 0; i<=21; i++)
            {
                let temp = Math.pow(angles[0] - fiveRings[i][0], 2) + Math.pow(angles[1] - fiveRings[i][1], 2);
                if(temp < sum)
                {
                    sum = temp;
                    pos = i;
                }
            }

            //Check if it is a tight match to the ideal conformation
            if((fiveRings[pos][0]-range) < angles[0] && angles[0] < (fiveRings[pos][0]+range) &&
                (fiveRings[pos][1]-range) < angles[1] && angles[1] < (fiveRings[pos][1]+range))
            {
                pos *= -1;
            }

            return pos;
        }

        private static updateColour(rgb:number[] , colour:number[])
        {
            rgb[0] = colour[0];
            rgb[1] = colour[1];
            rgb[2] = colour[2];
        }

        private static emptyArray(N:number):number[]
        {
            return this.emptyArrayCustom(N,0);
        }

        private static emptyArrayCustom(N:number, defValue:number):number[]
        {
            let out:number[] = [];
            for (let i=0; i<N; i++)
                out[i] = defValue;
            return out;
        }

        private static emptyArray2D(N:number, NP:number):number[][]
        {
            let out:number[][] = [[]];
            for (let i=0; i<NP; i++)
                for (let j=0; j<N; j++)
                    out[i][j] = 0;
            return out;
        }

        private static hill_reilly_ring_pucker(ring:SmallRing, positions:number[], framepos:number)
        {

            let N = ring.num();
            //MK do Hill-Reilly for 6-membered rings
            let NP = N-3; // number of puckering parameters
            let X:number[][] = this.emptyArray2D(3,N);
            let r:number[][] = this.emptyArray2D(3,N);
            let a:number[][] = this.emptyArray2D(3,NP);
            let q:number[][] = this.emptyArray2D(3,NP);
            let n:number[] = this.emptyArray(3);
            let p:number[] = this.emptyArray(3);
            let theta:number[] = this.emptyArray(NP);
             // atom co-ordinates
            // bond vectors
            // puckering axes
            // normalized puckering vectors
            // normal to reference plane
            // a flap normal
            // puckering parameters
            //float pucker_sum;
            //float max_pucker_sum;
            let atompos:number;
            let curatomid, i, j, k, l;

            // load ring co-ordinates
            for (i=0; i<N; i++)
            {
                curatomid = ring.get(i);
                atompos = framepos + 3*curatomid;
                X[i][0] = positions[atompos];
                X[i][1] = positions[atompos+1];
                X[i][2] = positions[atompos+2];
            }

            // calculate bond vectors
            for (i=0; i<N; i++)
            {
                j = (i+1) % N;
                this.vec_sub(r[3*i], X[3*j],X[3*i]);
            }

            // calculate puckering axes, flap normals and puckering vectors
            for (i=0; i<NP; i++)
            {
                k = (2*(i+1)) % N;
                j = (2*i) % N;
                l = (2*i+1) % N;
                this.vec_sub(a[i],X[k], X[j]);
                this.cross_prod(p,r[j],r[l]);
                this.cross_prod(q[i],a[i],p);
                this.vec_normalize(q[i]);
            }

            // reference normal
            this.cross_prod(n,a[0],a[1]);
            this.vec_normalize(n);

            // calculate the puckering parameters
            //pucker_sum = 0.0;

            for (i=0; i < NP; i++)
            {
                theta[i] = ((this.VMD_PI)/2.0) - Math.acos(this.dot_prod(q[i], n));
                theta[i] *= 57.2958; //convert to degrees
                //pucker_sum += theta[i];
            }

            return theta;  //pucker_sum;


        }

        // Calculate Hill-Reilly Pucker Parameters and convert these to a ring colour
        private static  hill_reilly_ring_color(ring:SmallRing, positions:number[] ,framepos:number, rgb:number[])
        {

            let colours:number[][] = [
                [1.0, 0.0, 0.0],  //Planar - Red
                [0.0, 1.0, 0.0],  //Chair - Green
                [0.0, 0.0, 1.0],  //Boat - Blue
                [0.75, 0.0, 1.0], //Half-chair - Purple
                [1.0, 0.5, 0.0],  //Skew-Boat - Orange
                [1.0, 1.0, 0.0],  //6-ring Envelope - Yellow
                [1.0, 0.4, 0.7],  //5-ring Twist - Pink
                [0.0, 1.0, 1.0],  //5-ring Envelope - Cyan
                [0.35, 0.2, 0.1], //7-ring - Brown
                [0.5, 0.5, 0.5]]; //Default - Grey

            let ringSize = ring.num();

            if(ringSize == 5 || ringSize == 6)
            {
                let pucker:number[] = this.hill_reilly_ring_pucker(ring, positions, framepos);
                let lighten = false;

                if(ringSize == 6) {
                    let pos:number = this.sixRingPosition(pucker);
                    if(pos < 0) {
                        lighten = true;
                        pos = pos * -1;
                    }

                    //Map to colour
                    if(pos == 0){	         	//Planar
                        this.updateColour(rgb, colours[0]); }
                    else if(pos <= 2){		//Chair
                        this.updateColour(rgb, colours[1]); }
                    else if(pos <= 8){		//Boat
                        this.updateColour(rgb, colours[2]); }
                    else if(pos <= 20){		//Half-Chair
                        this.updateColour(rgb, colours[3]); }
                    else if(pos <= 26){		//Skew-Boat
                        this.updateColour(rgb, colours[4]); }
                    else if(pos <= 38){		//Envelope
                        this.updateColour(rgb, colours[5]); }
                }

                if(ringSize == 5)
                {
                    let pos = this.fiveRingPosition(pucker);
                    if(pos < 0) {
                        lighten = true;
                        pos = pos * -1;
                    }

                    //Map to colour
                    if(pos == 0)
                    {	         	//Planar
                        this.updateColour(rgb, colours[0]);
                    }
                    else if(pos <= 10)
                    {		//Twist
                        this.updateColour(rgb, colours[6]);
                    }
                    else if(pos <= 20)
                    {		//Envelope
                        this.updateColour(rgb, colours[7]);
                    }
                }

                //Lighten colour if no close match was found
                if(lighten)
                {
                    rgb[0] = rgb[0] + (4 * (1.0 - rgb[0])) / 10;
                    rgb[1] = rgb[1] + (4 * (1.0 - rgb[1])) / 10;
                    rgb[2] = rgb[2] + (4 * (1.0 - rgb[2])) / 10;
                }
                pucker = [];
            }

            else if(ringSize == 7) //Brown
                this.updateColour(rgb, colours[8]);
            else //Default - Grey
                this.updateColour(rgb, colours[9]);

        }

        /*
         * Cremer-Pople pucker-parameter based coloring
         */

        // return sum + (x,y,z)
        private static vec_incr(sum:number[], x:number, y:number, z:number)
        {
            sum[0] += x;
            sum[1] += y;
            sum[2] += z;
        }

        private static vec_sub(a:number[], b:number[], c:number[])
        {
              a[0]=b[0]-c[0];
              a[1]=b[1]-c[1];
              a[2]=b[2]-c[2];
        }

        // Calculates cartesian axes based on coords of nuclei in ring
        // using cremer-pople algorithm. It is assumed that the
        // centre of geometry is the centre of the ring.
        private static ring_axes(X:number[], Y:number[], Z:number[], N:number, x:number[], y:number[], z:number[])
        {
            let Rp = [0.0, 0.0, 0.0];
            let Rpp = [0.0, 0.0, 0.0];
            let j;
            let VMD_PI = 3.14159265358979323846;
            for (j=0; j<N; j++)
            {
                let ze_angle = 2.0 * VMD_PI * j-1 / (N * 1.0);
                let ze_sin = Math.sin(ze_angle);
                let ze_cos = Math.cos(ze_angle);
                this.vec_incr(Rp, X[j]*ze_sin, Y[j]*ze_sin, Z[j]*ze_sin);
                this.vec_incr(Rpp, X[j]*ze_cos, Y[j]*ze_cos, Z[j]*ze_cos);
            }

            this.cross_prod(z, Rp, Rpp);
            this.vec_normalize(z);

            /*
             * OK, now we have z, the norm to the central plane, we need
             * to calculate y as the projection of Rp onto the plane
             * and x as y x z
             */
            let lambda = this.dot_prod(z, Rp);
            y[0] = Rp[0] - z[0]*lambda;
            y[1] = Rp[1] - z[1]*lambda;
            y[2] = Rp[2] - z[2]*lambda;
            this.vec_normalize(y);
            this.cross_prod(x, y, z);    // voila !
        }

        private static cross_prod(x1:number[], x2:number[], x3:number[])
        {
              x1[0] =  x2[1]*x3[2] - x3[1]*x2[2];
              x1[1] = -x2[0]*x3[2] + x3[0]*x2[2];
              x1[2] =  x2[0]*x3[1] - x3[0]*x2[1];
              return x1;
        }

        private static dot_prod(v1:number[], v2:number[])
        {
            return v1[0]* v2[0] + v1[1]* v2[1] + v1[2] * v2[2];
        }

         // normalize a vector, and return a pointer to it
         // Warning:  it changes the value of the vector!!
         private static vec_normalize(vect:number[])
         {
             let len2 = vect[0]*vect[0] + vect[1]*vect[1] + vect[2]*vect[2];

             // prevent division by zero
            if (len2 > 0)
            {
                let rescale = 1.0 / Math.sqrt(len2);
                vect[0] *= rescale;
                vect[1] *= rescale;
                vect[2] *= rescale;
            }

            return vect;
         }
         /*private static dihedral(a1:number[], a2:number[], a3:number[], a4:number[])
         {
             let r1:number[], r2:number[], r3:number[], n1:number[], n2:number[];
             this.vec_sub(r1, a2, a1);
             this.vec_sub(r2, a3, a2);
             this.vec_sub(r3, a4, a3);

             this.cross_prod(n1, r1, r2);
             this.cross_prod(n2, r2, r3);

             let psin = this.dot_prod(n1, r3) * Math.sqrt(this.dot_prod(r2, r2));
             let pcos = this.dot_prod(n1, n2);

             // atan2f would be faster, but we'll have to workaround the lack
             // of existence on some platforms.
             return 57.2958 *  Math.atan2(psin, pcos);
        }*/


        // Calculate distances of atoms from the mean ring plane
        private static atom_displ_from_mean_plane(X:number[], Y:number[], Z:number[], displ:number[], N:number)
        {
            let cog:number[] = [0.0, 0.0, 0.0];
            let x_axis:number[], y_axis:number[], z_axis:number[];
            let i;

            // calculate centre of geometry
            for (i=0; i<N; i++)
            {
                cog[0] += X[i]; cog[1] += Y[i]; cog[2] += Z[i];
            }
            cog[0] /= N;
            cog[1] /= N;
            cog[2] /= N;

            // centre the ring
            for (i=0; i<N; i++)
            {
                X[i] -= cog[0];
                Y[i] -= cog[1];
                Z[i] -= cog[2];
            }

            // @ts-ignore
            this.ring_axes( X, Y, Z, N, x_axis, y_axis, z_axis );

            // calculate displacement from mean plane
            for (i=0; i<N; i++)
            {
                // @ts-ignore
                displ[i] = X[i]*z_axis[0] + Y[i]*z_axis[1] + Z[i]*z_axis[2];
            }
        }


        // Calculate Cremer-Pople puckering parameters
        private static cremer_pople_params(N_ring_atoms:number, displ:number[],  q:number[], phi:number[], mQ:number[])
        {
            let i, j, k;
            if (N_ring_atoms < 3)
                return -1;

            let N = N_ring_atoms;
            phi[0]=0;  q[0]=0;  //no puckering parameters for m=1

            // if even no ring atoms, first calculate unpaired q puck parameter
            if (N % 2.0 == 0)
            {
                let sum = 0;
                mQ[0] = N_ring_atoms/2 - 1;

                for (i=0; i<N_ring_atoms; i++)
                    sum += displ[i]*Math.cos(i*this.VMD_PI);

                q[Math.floor(N_ring_atoms/2)-1]=Math.sqrt(1.0/N)*sum;
            }
            else
            {
                    mQ[0] = Math.floor(N_ring_atoms-1)/2;
            }

            // calculate paired puckering parameters
            for (i=1; i<mQ[0]; i++)
            {
                let q_cosphi = 0, q_sinphi = 0;
                for (j=0; j<N_ring_atoms; j++)
                {
                    q_cosphi += displ[j]*Math.cos(2.0*this.VMD_PI*((i+1)*j)/N);
                    q_sinphi += displ[j]*Math.sin(2.0*this.VMD_PI*((i+1)*j)/N);
                }

                q_cosphi *=  Math.sqrt(2.0/N);
                q_sinphi *= -Math.sqrt(2.0/N);
                phi[i] = Math.atan(q_sinphi/q_cosphi);

                if (q_cosphi < 0)
                    phi[i] += this.VMD_PI;
                else if (q_sinphi < 0)
                    phi[i] += 2.0*this.VMD_PI;

                q[i]=q_cosphi/phi[i];

                //calculate puckering amplitude
                mQ[1] = 0;
                for (k=0; k<N_ring_atoms; k++)
                    mQ[1] += displ[k] * displ[k];
                mQ[1] = Math.sqrt(mQ[1]);
            }

            return 1;
        }


        // Calculate Cremer-Pople Pucker Parameters and convert these to a ring colour
        private static cremer_pople_ring_color(ring:SmallRing, positions:number[], framepos:number, rgb:number[])
        {
            let N = ring.num(); //the number of atoms in the current ring
            let xring:number[] = [];
            let yring:number[] = [];
            let zring:number[] = [];
            let displ:number[] = [];
            let q:number[] = [];
            let phi:number[] = [];
            let mQ:number[] = [0,0];
            let atompos:number;
            let curatomid = 0;

            rgb = [0,0,0]; // set default color to black

            for(let i = 0; i < N; i++)
            {
                curatomid = ring.get(i);
                atompos = framepos + 3*curatomid;
                xring[i] = positions[atompos];
                yring[i] = positions[atompos + 1];
                zring[i] = positions[atompos + 2];
            }

            this.atom_displ_from_mean_plane(xring, yring, zring, displ, N);

            if (N==6)
            { //special case - pyranose rings
                if (this.cremer_pople_params(N, displ, q, phi, mQ))
                {
                    let cosTheta = q[2]/mQ[1];
                    let theta = Math.acos(cosTheta);
                    let sinTheta = Math.sin(theta);

                    // Q is the puckering amplitude - i.e. the intensity of the pucker.
                    // multiply by Q to show intensity, particularly for rings with
                    // little pucker (black)
                    // NOTE -using abs - polar positions therefore equivalent
                    // @ts-ignore
                    let intensity = Q;

                    rgb[0] = Math.abs(sinTheta)*intensity;
                    rgb[1] = Math.abs(cosTheta)*intensity;
                    rgb[2] = Math.abs(Math.sin(3.0*phi[1])*sinTheta)*intensity;
                }
            }
            else if (N==5)
            { //special case - furanose rings

                // @ts-ignore
                if (this.cremer_pople_params(N, displ, q, phi, mQ))
                {
                    rgb[0] = 0;
                    rgb[1] = 0;
                    rgb[2] = mQ[1];
                }
            }

            // clamp color values to legal range
            this.clamp_color(rgb);

        }

        private static clamp_color(rgb:number[])
        {
            // clamp color values to legal range
            if (rgb[0] < 0.0)
                rgb[0] = 0.0;
            if (rgb[0] > 1.0)
                rgb[0] = 1.0;
            if (rgb[1] < 0.0)
                rgb[1] = 0.0;
            if (rgb[1] > 1.0)
                rgb[1] = 1.0;
            if (rgb[2] < 0.0)
                rgb[2] = 0.0;
            if (rgb[2] > 1.0)
                rgb[2] = 1.0;
        }

    }

    class SmallRing
    {
        atoms:number[];
        orientated: number;

        num():number { return this.atoms.length; }
        get(i:number){ return this.atoms[i] }
        append(i:number) { this.atoms.push(i); }
        last_atom():number { return this.atoms[this.atoms.length-1]; }
        first_atom():number { return this.atoms[0]; }
        closed() { return this.first_atom() == this.last_atom(); }
        remove_last() { this.atoms.pop();}

        clear()
        {
            this.atoms = [];
            this.orientated = 0;
        }

        constructor()
        {
            this.atoms = [];
            this.orientated = 0;
        }

        setup(atomList:number[], o:number)
        {
            this.atoms = atomList;
            this.orientated = o;
        }


        copy():SmallRing
        {
            let temp:SmallRing = new SmallRing();
            temp.setup([...this.atoms], this.orientated);
            return temp;
        }

        reverse()
        {
             this.atoms.reverse();
        }

    }

    class LinkagePath
    {
        path:SmallRing;
        start_ring:number;
        end_ring:number;

        constructor()
        {
            this.path = new SmallRing();
            this.start_ring = 0;
            this.end_ring = 0;
        }

        setup(p_path:SmallRing, p_start_ring:number, p_end_ring:number)
        {
            this.path = p_path;
            this.start_ring = p_start_ring;
            this.end_ring = p_end_ring;
        }

        num():number { return this.path.num(); }
        get(i:number):number { return this.path.get(i); }
        append(i:number) { this.path.append(i); }
        remove_last() { this.path.remove_last(); }

        copy():LinkagePath
        {
            let pathcopy:LinkagePath;

            pathcopy = new LinkagePath();
            pathcopy.setup(this.path.copy(), this.start_ring, this.end_ring);
            return pathcopy;
        }

    }

class LinkageEdge
{
    left_atom:number;
    right_atom:number;
    paths:LinkagePath[];

    constructor(p_atom_left:number, p_atom_right:number)
    {
        this.left_atom = p_atom_left;
        this.right_atom = p_atom_right;
    }

    addPath(lp:LinkagePath)
    {
        this.paths.push(lp);
    }

}

class SmallRingLinkages
{
    edges_to_links:{[id:number]:number};
    links:LinkageEdge[];
    paths:LinkagePath[];

    constructor()
    {
        this.edges_to_links = [];
    }

    addLinkagePath(lp:LinkagePath)
    {
        let i:number, atom_left:number, atom_right:number;
        let link:LinkageEdge;
        atom_right = lp.path.get(0);

        for (i=1;i<lp.path.num();i++)
        {
            atom_left = atom_right;
            atom_right = lp.path.get(i);
            link = this.getLinkageEdge(atom_left,atom_right);
            link.addPath(lp);
        }

        this.paths.push(lp);
    }

    sharesLinkageEdges(lp:LinkagePath)
    {
        let i:number, atom_left:number, atom_right:number;
        let link:LinkageEdge;
        atom_right = lp.path.get(0);

        for (i = 1; i < lp.path.num(); i++)
        {
            atom_left = atom_right;
            atom_right = lp.path.get(i);
            link = this.getLinkageEdge(atom_left,atom_right);
            if (link.paths.length > 1)
                return true;
        }

        return false;
    }

    getLinkageEdge(atom_left:number, atom_right:number):LinkageEdge
    {
        let key, link_idx;
        let link:LinkageEdge;

        let atoms:number[] = [atom_left, atom_right];
        this.order_edge_atoms(atoms);
        atom_left = atoms[0];
        atom_right = atoms[1];
        key = this.get_link_key(atom_left, atom_right);

        link_idx = this.edges_to_links[key];
        if (link_idx == undefined)
        {
            link = new LinkageEdge(atom_left,atom_right);
            this.links.push(link);
            link_idx = this.links.length - 1;
            this.edges_to_links[key] = link_idx;
        }

        return this.links[link_idx];
    }

    order_edge_atoms(atoms:number[])
    {
        let t:number;
        if (atoms[0] > atoms[1])
        {
            t = atoms[0];
            atoms[0] = atoms[1];
            atoms[1] = t;
        }
    }

    get_link_key(al:number, ar:number):number
    {
        // al - left atom id
        // ar - right atom id
        // triangular mapping of pairs to single numbers
        return 1 + ar*(ar+1)/2 + al*(al+1)/2 + (al+1)*ar;
    }


}

    class MolAtom
    {
        public static MAX_ATOMS = 12;
        private static ATOMNORMAL = 0;
        private static ATOMPROTEINBACK =1;
        private static ATOMNUCLEICBACK =2;
        private static ATOMHYDROGEN    =3;
        private static RESNOTHING      =0;
        private static RESPROTEIN      =1;
        private static RESNUCLEIC      =2;
        private static RESWATERS       =3;
        // XXX contents of the Atom structure are ordered specifically so
        // that the compiler will pack it efficiently.

        // items that make this particular atom unique and are absolutely
        // needed to link it up to the rest of the structure, or are speed-critical
        nameindex:number;
        typeindex:number;
        uniq_resid:number;
        bondTo:number[];
        bonds:number;
        atomicnumber:number;
        altlocindex:number;
        insertionstr:string;

        // items which could potentially be moved into other data structures
        // to save memory, but are presently kept here for extra simplicity or speed
       chainindex:number;
       segnameindex:number;
       resid:number;
       resnameindex:number;

        // ATOMNORMAL, ATOMPROTEINBACK, ATOMNUCLEICBACK, ATOMHYDROGEN
        // XXX this should be converted to an unsigned bit-field to save memory
        atomType:number;

        // RESNOTHING, RESPROTEIN, RESNUCLEIC, RESWATERS
        // XXX this should be converted to an unsigned bit-field to save memory
        residueType:number;

        constructor() {
        }

        init(theresid:number, insertion:string)
        {
            this.uniq_resid = 0; // don't know yet, found in BaseMolecule
            this.bonds = 0;
            this.resid = theresid;
            this.insertionstr = insertion + "";
            this.insertionstr = this.insertionstr.charAt(0) + '\0';
            this.nameindex = this.typeindex = this.resnameindex = this.segnameindex = this.altlocindex = (-1);
            this.atomicnumber = (-1);

            for (let i = 0; i < MolAtom.MAX_ATOMS; i++)
            {
                this.bondTo[i] = -1;
            }
            this.atomType = MolAtom.ATOMNORMAL;
            this.residueType = MolAtom.RESNOTHING;
        }

        add_bond(a:number, type:number):number
        {
            if(this.bonds >= MolAtom.MAX_ATOMS) // fail
                return -1;

            this.bondTo[this.bonds] = a;
            if (type == MolAtom.ATOMPROTEINBACK || type == MolAtom.ATOMNUCLEICBACK)
                this.atomType = type;
            this.bonds++;
            return 0;
        }

        bonded(a:number): boolean
        {
            for (let i=0; i < this.bonds; i++)
            {
                if (this.bondTo[i] == a) {
                    return true;
                }
            }

            return false;
        }

    }

    export function buildGeometry(model: Core.Structure.Molecule.Model, parameters: Parameters,
        atomIndices: number[], ctx: Core.Computation.Context): Promise<PaperChainGeometry> {
        return PaperChainGeometryBuilder.build(model, parameters, atomIndices, ctx);
    }

    export class PaperChainGeometry extends GeometryBase
    {
        atomsGeometry: THREE.BufferGeometry = <any>void 0;
        bondsGeometry: THREE.BufferGeometry = <any>void 0;
        pickGeometry: THREE.BufferGeometry = <any>void 0;
        atomVertexMap: Selection.VertexMap = <any>void 0;
        vertexStateBuffer: THREE.BufferAttribute = <any>void 0;

        dispose()
        {
            this.atomsGeometry.dispose();
            this.bondsGeometry.dispose();
            this.pickGeometry.dispose();
        }
    }
}