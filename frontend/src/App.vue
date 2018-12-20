<template>
  <v-app>

    <v-toolbar app color="primary lighten-1">
      <v-toolbar-title class="headline">Memeify</v-toolbar-title>
    </v-toolbar>
    <v-content>
        <ImageForm/>
    </v-content>

    <v-footer
      height="auto"
      color="primary lighten-2"
    >
      <v-flex
        py-3
        text-xs-center
        xs12
      >
        &copy;2018 — <strong>Memeify</strong>
      </v-flex>
    </v-footer>

  </v-app>
</template>

<script>
import ImageForm from './components/ImageForm'

export default {
  name: 'App',
  components: {
    ImageForm
  },
  data () {
    return {
      //
    }
  }
}
</script>
