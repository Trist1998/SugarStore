<template>
  <v-app>
    <v-app-bar app color="primary" dark>
      <v-spacer></v-spacer>

      <router-link to="/create/user" v-if="!currentUser">
        <v-btn target="_blank" text>
          <span class="mr-2" >Create Account</span>
        </v-btn>
      </router-link>
      <router-link to="/login" v-if="!currentUser">
        <v-btn target="_blank" text>

          <span class="mr-2" >Login</span>
        </v-btn>
      </router-link>
      <div  v-if="currentUser">Signed In</div>
    </v-app-bar>
    <v-content>
      <router-view> </router-view>
    </v-content>
  </v-app>
</template>

<script>
export default {
  name: "App",

  components: {},
  computed: {
    currentUser()
    {
        return this.$store.state.auth.user;
    }
  }
};

</script>
