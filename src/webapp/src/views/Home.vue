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

</script>
