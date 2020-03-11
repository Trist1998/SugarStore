<template>
    <div class="login">
        <v-container class="newUser">
            <v-layout justify-center>
                <v-card raised min-width="30%" min-height="61%" max-height="61%">
                    <v-form @submit.prevent="handleLogin">
                        <v-container class="mb-2">
                            <v-row>
                                <v-col align="center" class="pa-1">
                                    <h1>Login</h1>
                                </v-col>
                            </v-row>
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
                                    <v-btn large color="primary" type="submit">
                                        Submit
                                    </v-btn>
                                </v-col>
                            </v-row>
                        </v-container>
                    </v-form>
                </v-card>
            </v-layout>
        </v-container>
    </div>
</template>

<script>
    import User from "../../../models/user/user";

    export default {
        name: 'login',
        data() {
            return {
                user: new User('', ''),
                loading: false,
                message: ''
            };
        },
        computed: {
            loggedIn()
            {
                return this.$store.state.auth.status.loggedIn;
            }
        },
        created()
        {
            if (this.loggedIn)
            {
                this.$router.push('/profile');
            }
        },
        methods: {
            handleLogin()
            {
                this.loading = true;
                var isValid = true;
                if (!isValid)
                {
                    this.loading = false;
                    return;
                }

                if (this.user.username && this.user.password)
                {
                    this.$store.dispatch('auth/login', this.user).then(
                        () => {
                            this.$router.push('/profile');
                        },
                        error => {
                            this.loading = false;
                            this.message =
                                (error.response && error.response.data) ||
                                error.message ||
                                error.toString();
                        }
                    );
                }

            }
        }

    };
</script>
