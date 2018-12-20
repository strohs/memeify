<template>
  <v-container>

    <v-layout justify-center column>
      <v-flex xs12 sm8 md4>

        <v-img
          :src="imageSrc" contain>
        </v-img>

        <v-alert
          :value="showAlert"
          color="error"
          icon="warning"
          outline
        >
          {{ alertMsg }}
        </v-alert>

        <v-form ref="form" v-model="valid" lazy-validation>
          <v-text-field
            v-model="topText"
            :rules="topTextRules"
            :counter="75"
            label="Top Text"
            required
          ></v-text-field>
          <v-text-field
            v-model="botText"
            :rules="botTextRules"
            :counter="75"
            label="Bottom Text"
            required
          ></v-text-field>

          <v-layout row>
            <v-file name="image" accept=".jpg,.jpeg,.png" @change="onImageSelect">
              <v-btn fab small>
                <v-icon>add_photo_alternate</v-icon>
              </v-btn>
            </v-file>
            <v-text-field
              label="Upload Image"
              :value="imageFile.name"
              disabled
            ></v-text-field>
          </v-layout>

          <v-btn
            :disabled="!valid"
            @click="submit"
          >
            Memeify!
          </v-btn>
          <v-btn @click="clear">clear</v-btn>
        </v-form>
      </v-flex>
    </v-layout>

  </v-container>
</template>

<script>
import axios from 'axios'
import VFile from '@outluch/v-file'

export default {
  name: 'ImageForm',

  components: {
    'v-file': VFile
  },

  data: () => ({
    valid: true,
    showAlert: false,
    alertMsg: '',
    apiUrl: process.env.VUE_APP_API_GW_URL,
    topText: '',
    botText: '',
    imageSrc: 'grumpy-cat.jpg',
    imageFile: { name: 'select a file to upload' },
    topTextRules: [
      v => !!v || 'top text is required',
      v => (v && v.length <= 75) || `Top Text must be less than 75 characters`
    ],
    botTextRules: [
      v => !!v || 'bottom text is required',
      v => (v && v.length <= 75) || `Bottom Text must be less than 75 characters`
    ]
  }),

  methods: {
    submit () {
      if (this.$refs.form.validate() && this.validImageFile(this.imageFile)) {
        const data = new FormData()
        data.append('topText', this.topText)
        data.append('botText', this.botText)
        data.append('image', this.imageFile)
        axios.post(this.apiUrl, data)
          .then(resp => {
            console.log('response', resp)
            this.imageSrc = resp.data.imageUrl
          })
          .catch(err => {
            console.log('error message', err)
          })
      }
    },
    clear () {
      this.$refs.form.reset()
    },
    onImageSelect (file) {
      console.log(file)
      // preview the image
      const reader = new FileReader()
      reader.addEventListener('load', () => {
        this.imageSrc = reader.result
      }, false)
      reader.readAsDataURL(file)
      this.imageFile = file
    },
    validImageFile (file) {
      // images must me <= 1MB
      if (file.size > 1048576) {
        this.alertMsg = `image size must be <= 1MB (selected image is ${Math.round(file.size / 1048576)}MB)`
        this.showAlert = true
        return false
      } else {
        this.showAlert = false
        return true
      }
    }
  }
}
</script>

<style scoped>

</style>
