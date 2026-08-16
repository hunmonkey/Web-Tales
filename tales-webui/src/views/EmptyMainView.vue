<template>
  <!-- The app opens onto nothing by owner request (SV-1201): the main area
       stays empty until a side-menu entry is chosen. Visual treatment of this
       state is SV-1211's decision, not this ticket's. -->
  <div class="app-shell__empty"/>
</template>

<script lang="ts">
import Vue from 'vue'

export default Vue.extend({
  name: 'EmptyMainView',
})
</script>
