// Internal value uses a decimal point; the input uses Vietnamese grouping and decimal comma.
export function formatCashValue(value) {
  if(value==null || value==='')return ''
  const [integer,fraction]=String(value).split('.')
  return integer.replace(/\B(?=(\d{3})+(?!\d))/g,'.')+(fraction===undefined?'':','+fraction)
}
export function updateCashInput(event,current) {
  const input=event.target,source=input.value,position=input.selectionStart??source.length
  const significantBefore=source.slice(0,position).replace(/[^0-9,-]/g,'').length
  const clean=source.replace(/[.\s\u00a0₫đ]/g,'')
  let next=current
  if(/^-?\d*(?:,\d{0,2})?$/.test(clean))next=clean.replace(',','.').replace(/^(-?)\./,(_,sign)=>sign+'0.').replace(/^(-?)0+(?=\d)/,'$1')
  const display=formatCashValue(next)
  input.value=display
  let caret=0,count=0
  while(caret<display.length && count<significantBefore){if(/[0-9,-]/.test(display[caret]))count++;caret++}
  input.setSelectionRange(caret,caret)
  return next
}
