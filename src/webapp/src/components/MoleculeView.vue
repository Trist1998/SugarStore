<template>
    <div id="container"></div>
</template>

<script>
    import * as THREE from 'three';
    import * as PDBLoader from '@/plugins/PDBLoader.js';

    var object;
    var scene;
    var camera;
    var renderer;
    var root;

    var loader = new THREE.PDBLoader();
    var offset = new THREE.Vector3();

    export default {
        name: "MoleculeView",
        mounted()
        {

            window.addEventListener( 'resize', onWindowResize, false );
            let container = document.getElementById('container');
            scene = new THREE.Scene();
            scene.background = new THREE.Color( 0x050505 );

            camera = new THREE.PerspectiveCamera( 70, window.innerWidth / window.innerHeight, 1, 5000 );
            camera.position.z = 1000;
            scene.add( camera );

            var light = new THREE.DirectionalLight( 0xffffff, 0.8 );
            light.position.set( 1, 1, 1 );
            scene.add( light );

            light = new THREE.DirectionalLight( 0xffffff, 0.5 );
            light.position.set( - 1, - 1, 1 );
            scene.add( light );

            root = new THREE.Group();
            scene.add( root );

            //

            renderer = new THREE.WebGLRenderer( { antialias: true } );
            renderer.setPixelRatio( window.devicePixelRatio );
            renderer.setSize( window.innerWidth, window.innerHeight );
            container.appendChild( renderer.domElement );


            // load a PDB resource
            loader.load(
                // resource URL
                'caffeine.pdb',
                // called when the resource is loaded
                function (geometryAtoms, geometryBonds, json)
                {

                    var boxGeometry = new THREE.BoxBufferGeometry( 1, 1, 1 );
                    var sphereGeometry = new THREE.IcosahedronBufferGeometry( 1, 2 );

                    geometryAtoms.computeBoundingBox();
                    geometryAtoms.boundingBox.getCenter( offset ).negate();

                    geometryAtoms.translate( offset.x, offset.y, offset.z );
                    geometryBonds.translate( offset.x, offset.y, offset.z );

                    var positions = geometryAtoms.vertices;
                    var colors = geometryAtoms.colors;

                    var position = new THREE.Vector3();
                    var color = new THREE.Color();

                    for ( var j = 0; j < positions.length; j ++ )
                    {

                        position.x = positions[j].x;
                        position.y = positions[j].y;
                        position.z = positions[j].z;

                        color.r = colors[j].r;
                        color.g = colors[j].g;
                        color.b = colors[j].b;

                        var material = new THREE.MeshPhongMaterial( { color: color } );

                        var object = new THREE.Mesh( sphereGeometry, material );
                        object.position.copy( position );
                        object.position.multiplyScalar( 75 );
                        object.scale.multiplyScalar( 25 );
                        root.add( object );

                    }

                    positions = geometryBonds.vertices;

                    var start = new THREE.Vector3();
                    var end = new THREE.Vector3();

                    for ( var i = 0; i < positions.length; i += 2 ) {

                        start.x = positions[i].x;
                        start.y = positions[i].y;
                        start.z = positions[i].z;

                        end.x = positions[i + 1].x;
                        end.y = positions[i + 1].y;
                        end.z = positions[i + 1].z;

                        start.multiplyScalar( 75 );
                        end.multiplyScalar( 75 );

                        object = new THREE.Mesh( boxGeometry, new THREE.MeshPhongMaterial( {color: 0xffffff} ) );
                        object.position.copy( start );
                        object.position.lerp( end, 0.5 );
                        object.scale.set( 5, 5, start.distanceTo( end ) );
                        object.lookAt( end );
                        root.add( object );

                    }

                    render();
                }

            );
        }

    }

    function animate()
    {
        var time = Date.now() * 0.0004;
        root.rotation.x = time;
        root.rotation.y = time * 0.7;
    }
    function render()
    {
        // render using requestAnimationFrame
        animate();
        requestAnimationFrame(render.bind(this));
        renderer.render(scene, camera);

    }

    function onWindowResize()
    {
        camera.aspect = window.innerWidth / window.innerHeight;
        camera.updateProjectionMatrix();

        renderer.setSize( window.innerWidth, window.innerHeight );

        render();
    }

</script>

<style scoped>

</style>