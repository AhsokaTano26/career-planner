<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'

const props=withDefaults(defineProps<{ modelValue:string; options:{value:string;label:string}[]; placeholder?:string; disabled?:boolean; required?:boolean }>(),{placeholder:'请选择',disabled:false,required:false})
const emit=defineEmits<{ 'update:modelValue':[value:string] }>()
const open=ref(false), root=ref<HTMLElement|null>(null)
const selected=computed(()=>props.options.find(item=>item.value===props.modelValue))
function close(){open.value=false}
function toggle(){if(!props.disabled)open.value=!open.value}
function choose(value:string){emit('update:modelValue',value);close()}
function keydown(event:KeyboardEvent){if(event.key==='Escape')return close();if(event.key==='Enter'||event.key===' '){event.preventDefault();toggle();return}if(!open.value||!['ArrowDown','ArrowUp'].includes(event.key))return;event.preventDefault();const index=Math.max(0,props.options.findIndex(item=>item.value===props.modelValue));const next=event.key==='ArrowDown'?Math.min(props.options.length-1,index+1):Math.max(0,index-1);choose(props.options[next].value)}
function outside(event:MouseEvent){if(root.value&&!root.value.contains(event.target as Node))close()}
document.addEventListener('click',outside)
onBeforeUnmount(()=>document.removeEventListener('click',outside))
</script>
<template><div ref="root" class="base-select" :class="{open,disabled}"><input class="base-select-value" tabindex="-1" :value="modelValue" :required="required" @invalid="open=true"><button type="button" class="base-select-trigger" :aria-expanded="open" @click="toggle" @keydown="keydown"><span :class="{placeholder:!selected}">{{selected?.label||placeholder}}</span><i>⌄</i></button><div v-if="open" class="base-select-menu" role="listbox"><button v-for="option in options" :key="option.value" type="button" role="option" :aria-selected="option.value===modelValue" :class="{selected:option.value===modelValue}" @click="choose(option.value)">{{option.label}}</button></div></div></template>
