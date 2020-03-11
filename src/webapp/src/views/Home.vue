<template>
  <div class="home">
      <MoleculeView/>
  </div>
</template>


<script>
// @ is an alias to /src
import MoleculeView from "@/components/MoleculeView.vue";

export default {
  name: "home",
  components: {
    MoleculeView
  }
};
/*import * as THREE from 'three';
import * as PDBLoader from '@/plugins/PDBLoader.js';
var scene = new THREE.Scene();
var camera = new THREE.PerspectiveCamera( 75, window.innerWidth / window.innerHeight, 0.1, 1000 );
var renderer = new THREE.WebGLRenderer();

renderer.setSize( window.innerWidth, window.innerHeight );
renderer.setClearColor(new THREE.Color(0xAAAA));

camera.position.x = 0;
camera.position.y = 0;
camera.position.z = 10;
camera.lookAt(new THREE.Vector3(0, 0, 0));

var dir1 = new THREE.DirectionalLight(0.4);
dir1.position.set(-30, 30, -30);
scene.add(dir1);

var dir2 = new THREE.DirectionalLight(0.4);
dir2.position.set(-30, 30, 30);
scene.add(dir2);

var dir3 = new THREE.DirectionalLight(0.4);
dir3.position.set(30, 30, -30);
scene.add(dir3);

// add spotlight for the shadows
var spotLight = new THREE.SpotLight(0xffffff);
spotLight.position.set(30, 30, 30);
scene.add(spotLight);

document.body.appendChild( renderer.domElement );

// call the render function
var step = 0;

var mesh;

var loader = new THREE.PDBLoader();
var group = new THREE.Object3D();

// load a PDB resource
loader.load(
    // resource URL
    'glucose.pdb',
    // called when the resource is loaded
    function ( geometry, geometryBonds )
    {
        var i = 0;

        geometry.vertices.forEach(
            function (position)
            {
                var sphere = new THREE.SphereGeometry(0.3);
                var material = new THREE.MeshPhongMaterial({color: geometry.colors[i++]});
                var mesh = new THREE.Mesh(sphere, material);
                mesh.position.copy(position);
                group.add(mesh);
            }
        );

        for (var j = 0; j < geometryBonds.vertices.length; j += 2)
        {
            var path = new THREE.CatmullRomCurve3([geometryBonds.vertices[j], geometryBonds.vertices[j + 1]]);
            var tube = new THREE.TubeGeometry(path, 1, 0.5);
            var material = new THREE.MeshPhongMaterial({color: 0x0000});
            var mesh = new THREE.Mesh(tube, material);
            group.add(mesh);
        }

        scene.add(group);
    }
);

render();

function render()
{
   if (group)
    {
        group.position.x = 0;
        group.position.y = 0;
        group.position.z = 0;
        group.rotation.y += 0.006;
        group.rotation.x += 0.006;
    }

    // render using requestAnimationFrame
    requestAnimationFrame(render);
    renderer.render(scene, camera);
}*/
</script>
