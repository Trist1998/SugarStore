<template>
  <v-container class="newUser">
    <v-layout justify-center>
      <v-card raised min-width="61%" min-height="61%" max-height="61%">
        <v-form>
          <v-container class="mb-2">
            <v-row>
              <v-col align="center" class="pa-1">
                <h1>Create Account</h1>
              </v-col>
            </v-row>
            <!--<v-row justify-center>
              <v-col align="center">
                <v-text-field
                  v-model="name"
                  label="First Name"
                  placeholder="Enter your first name"
                  outlined
                >
                </v-text-field>
              </v-col>
            </v-row>
            <v-row justify-center>
              <v-col align="center">
                <v-text-field
                        v-model="surname"
                  label="Surname"
                  placeholder="Enter your surname"
                  outlined
                >
                </v-text-field>
              </v-col>
            </v-row>-->
            <v-row justify-center>
              <v-col align="center">
                <v-text-field
                        v-model="user.username"
                        label="Username"
                        placeholder="Enter your username"
                        outlined
                >
                </v-text-field>
              </v-col>
            </v-row>
            <v-row justify-center>
              <v-col align="center">
                <v-text-field
                  v-model="user.email"
                  label="Email Address"
                  placeholder="Enter your email address"
                  outlined
                >
                </v-text-field>
              </v-col>
            </v-row>
           <!-- <v-row justify-center>
              <v-col align="center" class="pa-auto">
                <v-text-field
                  v-model="cellphoneNo"
                  label="Cellphone Number"
                  placeholder="Enter your Cellphone number"
                  outlined
                >
                </v-text-field>
              </v-col>
            </v-row>-->
            <v-row justify-center>
              <v-col align="center" class="pa-auto">
                <v-text-field :type="'password'"
                        v-model="user.password"
                        label="Password"
                        placeholder="Enter your Password"
                        outlined
                >
                </v-text-field>
              </v-col>
            </v-row>
            <v-row justify-center>
              <v-col align="center" class="pa-auto">
                <v-text-field :type="'password'"
                        v-model="confirmedPassword"
                        label="Confirm Password"
                        placeholder="Re-enter your Password"
                        outlined
                >
                </v-text-field>
              </v-col>
            </v-row>
            <v-row justify-center>
              <v-col align="center" class="pa-auto">
                <v-btn large color="primary" @click="handleRegister">
                  Submit
                </v-btn>
              </v-col>
            </v-row>
          </v-container>
        </v-form>
      </v-card>
    </v-layout>
  </v-container>
</template>

<script>
  import User from "../../../models/user/user";
  export default {
    name: 'Register',
    data() {
      return {
        user: new User('', '', ''),
        confirmedPassword: '',
        submitted: false,
        successful: false,
        message: ''
      };
    },
    computed: {
      loggedIn() {
        return this.$store.state.auth.status.loggedIn;
      }
    },
    mounted() {
      if (this.loggedIn) {
        this.$router.push('/profile');
      }
    },
    methods: {
      handleRegister()
      {
        this.message = '';
        this.submitted = true;
        const isValid = true;
        if (isValid)
        {
            this.$store.dispatch('auth/register', this.user).then(
                data => {
                  this.message = data.message;
                  this.successful = true;
                },
                error => {
                  this.message =
                          (error.response && error.response.data) ||
                          error.message ||
                          error.toString();
                  this.successful = false;
                }
          );
        }
      }
    }
  };
</script>
