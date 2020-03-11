<template>
  <v-container>
    <v-layout text-center wrap>
      <v-flex xs12>
        <v-img
          :src="require('../assets/logo.svg')"
          class="my-3"
          contain
          height="200"
        ></v-img>
      </v-flex>

      <v-flex mb-4>
        <h1 class="display-2 font-weight-bold mb-3">
          Welcome to LekR
        </h1>
        <span> {{ textData.name }}</span>
        <v-btn @click="fetchUser">Clcik</v-btn>
      </v-flex>
    </v-layout>
  </v-container>
</template>

<script>
  import axios from 'axios';
export default {
  name: "HelloWorld",
  data: () => ({
    textData: "More text"
  }),

  methods: {
    fetchUser() {
      axios.get("http://localhost:80/user/tristanmwood@hotmail.com")
        .then((response) => {
          this.textData = response.data;
        });
    }
  }
};
</script>
