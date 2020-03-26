<template>
        <v-container id="container" justify-center fluid>
                <v-row justify="center">
                    <v-col cols="12">
                        <v-row justify="center" class="pa-6">
                            <v-col cols="6">
                                <v-text-field
                                        v-model="casperInput"
                                        label="Capser"
                                        placeholder="Enter your Casper"
                                        outlined
                                ></v-text-field>
                            </v-col>
                            <v-col cols="6">
                                <v-text-field
                                        v-model="noRepeatingUnits"
                                        label="Repeating Units"
                                        placeholder="Enter the number of repeating units"
                                        outlined
                                ></v-text-field>

                            </v-col>
                        </v-row>
                    </v-col>
                    <v-col cols="12">
                        <v-row justify="center">
                            <molecule-window ref="mwindow"/>
                        </v-row>
                    </v-col>
                </v-row>
            <v-row>
                <v-col cols="12">
                    <v-row justify="center">
                        <v-btn v-on:click="loadCasper" class="blue v-size--large">Render</v-btn>
                    </v-row>
                </v-col>
            </v-row>
        </v-container>
</template>

<script>
    const API_URL = 'http://localhost:80/';
    import MoleculeWindow from "./MoleculeWindow";
    import axios from 'axios';
    export default {
        name: "MoleculeView",
        components: {MoleculeWindow},
        data (){
            return {
                casperInput: "",
                noRepeatingUnits: 1
            }
        },
        methods: {
            loadCasper()
            {
                axios.post(API_URL + 'carbbuilder/build', {
                    casperInput: this.casperInput,
                    noRepeatingUnits: this.noRepeatingUnits
                }).then((response) => {
                    this.$refs.mwindow.loadAndRender(response.data);
                });
            }

        }

    }
</script>

<style scoped>

</style>