function enhance(select:HTMLSelectElement){
  if(select.dataset.enhancedSelect) return
  select.dataset.enhancedSelect='true'
  const root=document.createElement('div'), trigger=document.createElement('button'), menu=document.createElement('div')
  root.className='native-select-renderer';trigger.type='button';trigger.className='native-select-trigger';menu.className='native-select-menu';menu.setAttribute('role','listbox')
  select.classList.add('native-select-source');select.insertAdjacentElement('afterend',root);root.append(trigger,menu)
  const render=()=>{const current=select.selectedOptions[0];trigger.replaceChildren(...[document.createTextNode(current?.textContent||'请选择'),(()=>{const caret=document.createElement('i');caret.textContent='⌄';return caret})()]);menu.replaceChildren(...[...select.options].map(option=>{const button=document.createElement('button');button.type='button';button.textContent=option.text;button.className=option.selected?'selected':'';button.disabled=option.disabled;button.onclick=()=>{select.value=option.value;select.dispatchEvent(new Event('change',{bubbles:true}));close();render()};return button}))}
  const close=()=>{root.classList.remove('open')};trigger.onclick=()=>{if(!select.disabled)root.classList.toggle('open')};select.addEventListener('change',render);new MutationObserver(render).observe(select,{childList:true,subtree:true,attributes:true});document.addEventListener('click',event=>{if(!root.contains(event.target as Node))close()});render()
}
export function installCustomSelects(){const scan=()=>document.querySelectorAll<HTMLSelectElement>('select').forEach(enhance);new MutationObserver(scan).observe(document.body,{childList:true,subtree:true});scan()}
