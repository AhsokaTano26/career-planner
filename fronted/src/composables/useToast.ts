import { ref } from 'vue'

export function useToast(){
  const message=ref('')
  let timer:number|undefined
  function show(next:string){window.clearTimeout(timer);message.value=next;timer=window.setTimeout(()=>message.value='',2200)}
  return { toast:message, show }
}
