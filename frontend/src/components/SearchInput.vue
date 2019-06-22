<template>
    <div class="search-input">
        <b-form @submit.prevent="goSearch">
            <div class="input">
              <b-form-input v-model="query" placeholder=""
                v-on:input="autoCompletion" list="auto-completion-list">
              </b-form-input>
              <a class="rec" v-if="speechRecognitionSupported" 
                href="javascript:;" v-on:click="speechRecognize">
              </a>              
            </div>
            <datalist id="auto-completion-list" autocomplete="off">
            <option v-for="candidate in candidates" v-bind:key="candidate">
                {{ candidate }}
            </option>
            </datalist>
            <b-button variant="primary" type="submit">
                Search
            </b-button>
            <b-button variant="primary" v-on:click="goAdvanced">
                Advanced
            </b-button>
        </b-form>
    </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'search-input',
  data () {
    return {
      query: '',
      queryBuffer: '',
      candidates: [],
      speechRecognitionSupported: false,
    }
  },
  methods: {
    goSearch () {
      this.$emit('newSearch', this.query)
    },
    goAdvanced () {
      this.$emit('goAdvanced')
    },
    updateCompletion (query) {
      this.queryBuffer = query
      axios.get(
        '/api/_completion', {
          params: { 'query': query }
        })
        .then((response) => {
          this.candidates = response.data.queries
        })
    },
    autoCompletion () {
      if (this.query === this.queryBuffer) {
        return
      }
      this.updateCompletion(this.query)
    },
    updateQuery (query) {
      this.query = query
      this.updateCompletion(query)
    },
    initSpeechRecognizer () {
      // Reference: https://www.google.com/intl/en/chrome/demos/speech.html

      this.speechRecognitionSupported = 'webkitSpeechRecognition' in window
      if (!this.speechRecognitionSupported) {
        return
      }

      this.recognizing = false

      var ignore_onend = false  

      const recognition = new webkitSpeechRecognition()
      this.recognition = recognition
      recognition.continuous = true;
      recognition.interimResults = true;    
      recognition.lang = 'cmn-Hans-CN'; // Hardcoded

      let _this = this

      recognition.onstart = function() {
        ignore_onend = false;
        _this.recognizing = true;
      };

      recognition.onerror = function(event) {
        ignore_onend = true;
      };

      recognition.onend = function() {
        _this.recognizing = false;
        if (ignore_onend) {
          ignore_onend = false;
          return;
        }
      };

      recognition.onresult = function(event) {
        if (typeof(event.results) == 'undefined') {
          recognition.onend = null;
          recognition.stop();
          upgrade();
          return;
        }
        for (var i = event.resultIndex; i < event.results.length; ++i) {
          if (event.results[i].isFinal) {
            _this.query += event.results[i][0].transcript;
            _this.updateQuery(_this.query)
          }
        }
      }     
    },
    speechRecognize () {
      if (this.recognizing) {
        this.recognition.stop()
        return
      }
      this.recognition.start()
    }
  },
  mounted () {
    this.initSpeechRecognizer()
  }
}
</script>

<style lang="scss" scoped>
.search-input {
  form {
    display: flex;
    justify-content: center;

    .input  {
      display: flex;
      width: 500px;
      vertical-align: middle;
      border: solid 1px #777;
      height: calc(2.25rem + 2px);

      input {
        display: inline-block;
        width: 460px;
        border: none;
        line-height: calc(2.25rem + 2px);
        vertical-align: middle;
        background: none;
        outline: none;
      }

      input.form-control:focus {
        outline: none;
        box-shadow: none;
      }

      .rec {
        display: inline-block;
        width: 24px;
        height: 38px;
        // height: calc(2.25rem + 2px);
        background: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA0AAAASCAYAAACAa1QyAAAAAXNSR0IArs4c6QAAAAlwSFlzAAALEwAACxMBAJqcGAAAAVlpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IlhNUCBDb3JlIDUuNC4wIj4KICAgPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICAgICAgPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIKICAgICAgICAgICAgeG1sbnM6dGlmZj0iaHR0cDovL25zLmFkb2JlLmNvbS90aWZmLzEuMC8iPgogICAgICAgICA8dGlmZjpPcmllbnRhdGlvbj4xPC90aWZmOk9yaWVudGF0aW9uPgogICAgICA8L3JkZjpEZXNjcmlwdGlvbj4KICAgPC9yZGY6UkRGPgo8L3g6eG1wbWV0YT4KTMInWQAAAXlJREFUKBW9kr1Kw1AUx5OYVWiRGqqLiPgIios4qg+QobhI7QdYH6CKk4IPUCRNQhFRHPIAnRykIIqre7cMsUMHp0Jt/J2ShiYWxx44/M/53/N17z2qkhLbtgtQl+g62kWvKpXKExiLFlsYzWbzCLgOw7A2HA6XVFU9Ez/i41A9tjAIutA0rVAqld4j/tl13cJoNLrHf5zEJjpBbvq+/zE5FIz8jWkuYXOfMEFETppPd5qV84ebX5Iu82azWd00zR+Zg+fmEdWZd/M8b6Hf7w9kvKDX661Eg3cdx9mK7DFQdBtDPlkhYRUI5J86uq4fgA4d6nR6sCzrOAiCN8MwduDvhAdF9tFX/lJrYNRbrdZiuVz2SDqHs/P5/EBQfOHlXOIkXsVQGMnicI2OZrFY/BZuWiSBtfLo6FPgZLxGmUymxrwNDj65ww3YzuVycleDQof4Ml6bONlFZdxpUpXF3MU+peIeuIx+McELeFutVjvg/5Jeneno+W3ELz3Rmg23qA6NAAAAAElFTkSuQmCC) no-repeat center;
      }
    }

    .btn {
      margin-left: 6px;
    }
  }
}
</style>
