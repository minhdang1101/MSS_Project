const TODAY = new Date("2026-10-26");
const RATE = 3000, GRACE = 1, CAP = 150000, LOCK = 50000;
const D = s => new Date(s);
const days = (a,b) => Math.round((b-a)/864e5);
const fmtD = d => d.toLocaleDateString("vi-VN",{day:"2-digit",month:"2-digit"});
const fmtStamp = d => d.toLocaleDateString("vi-VN",{day:"2-digit",month:"short"}).replace(/\./g,"");
const vnd = n => n.toLocaleString("vi-VN")+"đ";
const addDays = (d,n) => new Date(d.getTime()+n*864e5);

const books = [
  {isbn:"978-604-1-08765-2",t:"Số đỏ",a:"Vũ Trọng Phụng",fmt:"print",copies:4,out:3,price:85000},
  {isbn:"978-604-2-11203-7",t:"Dế mèn phiêu lưu ký",a:"Tô Hoài",fmt:"print",copies:6,out:6,price:62000},
  {isbn:"978-604-9-77310-1",t:"Nỗi buồn chiến tranh",a:"Bảo Ninh",fmt:"digital",copies:10,out:7,price:0},
  {isbn:"978-604-3-55012-4",t:"Lược sử thời gian",a:"Stephen Hawking",fmt:"print",copies:3,out:1,price:120000},
  {isbn:"978-604-1-90024-8",t:"Truyện Kiều",a:"Nguyễn Du",fmt:"print",copies:8,out:2,price:45000},
  {isbn:"978-604-9-40881-5",t:"Cơ sở dữ liệu phân tán",a:"Lê Văn Sơn",fmt:"digital",copies:25,out:25,price:0},
  {isbn:"978-604-7-66190-3",t:"Tắt đèn",a:"Ngô Tất Tố",fmt:"print",copies:5,out:0,price:58000},
];
const members = [
  {id:"TV-0412",n:"Phạm Minh Khôi",tier:"Sinh viên"},
  {id:"TV-0087",n:"Lê Hoài An",tier:"Tiêu chuẩn"},
  {id:"TV-1130",n:"Đỗ Bảo Ngọc",tier:"Sinh viên"},
  {id:"TV-0256",n:"Trần Quốc Việt",tier:"Giảng viên"},
  {id:"TV-0901",n:"Vũ Thanh Hà",tier:"Tiêu chuẩn"},
];
let loans = [
  {id:1,m:"TV-0412",isbn:"978-604-1-08765-2",copy:"Bản 2",out:"2026-09-28",due:"2026-10-12"},
  {id:2,m:"TV-0087",isbn:"978-604-2-11203-7",copy:"Bản 5",out:"2026-10-05",due:"2026-10-19"},
  {id:3,m:"TV-1130",isbn:"978-604-9-77310-1",copy:"Giấy phép 7",out:"2026-10-14",due:"2026-10-28"},
  {id:4,m:"TV-0256",isbn:"978-604-3-55012-4",copy:"Bản 1",out:"2026-10-13",due:"2026-10-27"},
  {id:5,m:"TV-0901",isbn:"978-604-1-90024-8",copy:"Bản 6",out:"2026-10-15",due:"2026-10-29"},
  {id:6,m:"TV-0412",isbn:"978-604-2-11203-7",copy:"Bản 1",out:"2026-08-30",due:"2026-09-13"},
];
let fines = [
  {m:"TV-0087",t:"Tắt đèn",late:4,amt:9000,paid:true},
  {m:"TV-0901",t:"Số đỏ",late:2,amt:3000,paid:false},
];
// Older stamps on each copy's slip (history)
const history = {
  "978-604-1-08765-2":[["2026-03-02","2026-03-16"],["2026-05-11","2026-05-25"],["2026-07-20","2026-08-03"]],
  "978-604-2-11203-7":[["2026-06-01","2026-06-15"],["2026-08-02","2026-08-16"]],
  "978-604-3-55012-4":[["2026-04-14","2026-04-28"]],
};
const book = i => books.find(b=>b.isbn===i);
const mem = i => members.find(m=>m.id===i);
const initials = n => n.split(" ").slice(-2).map(w=>w[0]).join("");

/* Fine rule: print only, grace day, daily rate, capped. */
function calcFine(loan, onDate){
  const b = book(loan.isbn);
  const late = Math.max(0, days(D(loan.due), onDate));
  if (b.fmt==="digital") return {late, billable:0, amt:0, digital:true};
  const billable = Math.max(0, late-GRACE);
  return {late, billable, amt:Math.min(CAP, billable*RATE), capped: billable*RATE>CAP};
}
function status(loan){
  const d = days(TODAY, D(loan.due));
  if (d<0) return {k:"late",txt:`Quá hạn ${-d} ngày`};
  if (d<=3) return {k:"soon",txt: d===0?"Đến hạn hôm nay":`Còn ${d} ngày`};
  return {k:"ok",txt:`Còn ${d} ngày`};
}
const owed = id => fines.filter(f=>f.m===id&&!f.paid).reduce((s,f)=>s+f.amt,0)
  + loans.filter(l=>l.m===id).reduce((s,l)=>s+calcFine(l,TODAY).amt,0);

/* ---------- nav ---------- */
const navBtns = document.querySelectorAll(".nav button");
function go(v){
  navBtns.forEach(b=>{ if(b.dataset.v===v) b.setAttribute("aria-current","page"); else b.removeAttribute("aria-current"); });
  document.querySelectorAll(".view").forEach(s=>s.classList.toggle("on",s.id===v));
  window.scrollTo(0,0);
}
navBtns.forEach(b=>b.onclick=()=>go(b.dataset.v));
document.querySelectorAll("[data-go]").forEach(b=>b.onclick=()=>go(b.dataset.go));

/* ---------- today ---------- */
function renderToday(){
  const late = loans.filter(l=>status(l).k==="late");
  const soon = loans.filter(l=>status(l).k==="soon");
  const accruing = late.reduce((s,l)=>s+calcFine(l,TODAY).amt,0);
  document.getElementById("nav-late").textContent = late.length || "";
  document.getElementById("strip").innerHTML = `
    <div><b>${loans.length}</b><span>lượt đang mượn</span></div>
    <div class="late"><b>${late.length}</b><span>lượt quá hạn</span></div>
    <div><b>${soon.length}</b><span>đến hạn trong 3 ngày</span></div>
    <div><b>${vnd(accruing)}</b><span>phạt đang tích luỹ</span></div>`;
  document.getElementById("today-late").innerHTML = late.length ? late.map(l=>{
    const f = calcFine(l,TODAY), b=book(l.isbn), m=mem(l.m);
    return `<li><div><div class="title">${b.t}</div><div class="meta">${m.n}, hạn trả ${fmtD(D(l.due))}</div></div>
      <div class="right"><span class="st late">${status(l).txt}</span><div class="meta">${vnd(f.amt)}</div></div></li>`}).join("")
    : `<li><div class="meta">Không có sách quá hạn.</div></li>`;
  document.getElementById("today-soon").innerHTML = soon.map(l=>{
    const b=book(l.isbn), m=mem(l.m);
    return `<li><div><div class="title">${b.t}</div><div class="meta">${m.n}${b.fmt==="digital"?", sách điện tử":""}</div></div>
      <div class="right"><span class="st soon">${status(l).txt}</span><div class="meta">Đã nhắc qua email</div></div></li>`}).join("");
}

/* ---------- desk ---------- */
let mode = "return";
document.querySelectorAll("[data-mode]").forEach(b=>b.onclick=()=>{
  mode=b.dataset.mode;
  document.querySelectorAll("[data-mode]").forEach(x=>x.setAttribute("aria-pressed",x===b));
  document.getElementById("form-return").hidden = mode!=="return";
  document.getElementById("form-borrow").hidden = mode!=="borrow";
  mode==="return" ? renderReturn() : renderBorrow();
});

function drawSlip(isbn, copy, current, extra){
  const b = book(isbn);
  document.getElementById("slip-title").textContent = b.t;
  document.getElementById("slip-copy").textContent = copy;
  const rows = (history[isbn]||[]).map(([o,d])=>({o:D(o),d:D(d),old:true}));
  if (current) rows.push(current);
  while(rows.length<6) rows.push(null);
  const cell = (r,key)=> r ? `<div class="cell"><span class="ink ${r.old?"old":""} ${r.isNew?"new":""} ${key==="d"&&r.red?"red":""}">${fmtStamp(r[key])}</span></div>` : `<div class="cell"></div>`;
  document.getElementById("slip-out").innerHTML = rows.map(r=>cell(r,"o")).join("");
  document.getElementById("slip-due").innerHTML = rows.map(r=>cell(r,"d")).join("");
  const bs = document.getElementById("bigstamp");
  bs.className = "bigstamp" + (extra? " on "+(extra.cls||"") : "");
  bs.textContent = extra? extra.txt : "";
}

const rLoan = document.getElementById("r-loan"), rDate = document.getElementById("r-date");
let pendingFine = null;
function fillLoans(done){
  rLoan.innerHTML = (done?`<option value="" selected>Đã nhận trả: ${done}</option>`:"") + loans.map(l=>`<option value="${l.id}">${book(l.isbn).t}, ${mem(l.m).n}</option>`).join("");
}
function renderReturn(stampTxt){
  const l = loans.find(x=>x.id==rLoan.value);
  if(!l){ document.getElementById("r-calc").innerHTML='<p class="note">Không còn lượt mượn nào.</p>'; return; }
  const m = mem(l.m), b = book(l.isbn), on = D(rDate.value), f = calcFine(l,on), st = status(l);
  const o = owed(m.id);
  document.getElementById("r-member").innerHTML = `<div class="avatar">${initials(m.n)}</div>
    <div><div><b>${m.n}</b> <span class="meta">${m.id}</span></div>
    <div class="${o>=LOCK?"warn":"meta"}">${o>=LOCK?"Thẻ đang bị khoá mượn, nợ "+vnd(o):"Nợ phạt hiện tại "+vnd(o)}</div></div>`;
  let html;
  if (f.digital){
    html = `<div class="row"><span>Định dạng</span><span class="st digital">Sách điện tử</span></div>
      <div class="row total"><span>Tiền phạt</span><span>0đ</span></div>
      <p class="note">Quyền đọc sẽ được thu hồi ngay khi nhận trả.</p>`;
  } else {
    html = `<div class="row"><span>Hạn trả</span><span>${fmtD(D(l.due))}</span></div>
      <div class="row"><span>Trả ngày</span><span>${fmtD(on)}</span></div>
      <div class="row"><span>Số ngày trễ</span><span>${f.late}</span></div>
      <div class="row"><span>Trừ ngày ân hạn</span><span>−${Math.min(GRACE,f.late)}</span></div>
      <div class="row"><span>${f.billable} ngày × ${vnd(RATE)}</span><span>${vnd(f.billable*RATE)}</span></div>
      ${f.capped?`<div class="row"><span>Áp mức trần</span><span>${vnd(CAP)}</span></div>`:""}
      <div class="row total"><span>Tiền phạt</span><span style="color:${f.amt?"var(--late)":"inherit"}">${vnd(f.amt)}</span></div>`;
  }
  document.getElementById("r-calc").innerHTML = html;
  drawSlip(l.isbn, l.copy, {o:D(l.out),d:D(l.due),red:st.k==="late"}, stampTxt);
}
rLoan.onchange = ()=>{ if(!rLoan.value) return; rLoan.querySelector('option[value=""]')?.remove(); document.getElementById("r-pay").disabled=true; document.getElementById("r-do").disabled=false; renderReturn(); };
rDate.onchange = ()=>renderReturn();
document.getElementById("r-do").onclick = ()=>{
  const l = loans.find(x=>x.id==rLoan.value); if(!l) return;
  const f = calcFine(l,D(rDate.value)), b=book(l.isbn);
  renderReturn(f.amt?{txt:"Trả trễ "+f.late+" ngày"}:{txt:"Đã trả",cls:"paid"});
  loans = loans.filter(x=>x!==l); b.out = Math.max(0,b.out-1);
  if (f.amt){ pendingFine={m:l.m,t:b.t,late:f.late,amt:f.amt,paid:false}; fines.unshift(pendingFine); document.getElementById("r-pay").disabled=false; }
  toast(f.amt?`Đã nhận trả. Ghi nợ phạt ${vnd(f.amt)}.`:"Đã nhận trả sách.");
  fillLoans(b.t); document.getElementById("r-do").disabled = true; renderAll(false);
};
document.getElementById("r-pay").onclick = e=>{
  if(!pendingFine) return; pendingFine.paid=true;
  toast(`Đã thu ${vnd(pendingFine.amt)}.`); pendingFine=null; e.target.disabled=true; renderAll(false);
};

const bMember=document.getElementById("b-member"), bBook=document.getElementById("b-book"), bDays=document.getElementById("b-days");
function renderBorrow(fresh){
  const m = mem(bMember.value), b = book(bBook.value), o = owed(m.id);
  const locked = o>=LOCK, none = b.out>=b.copies;
  document.getElementById("b-chip").innerHTML = `<div class="avatar">${initials(m.n)}</div>
    <div><div><b>${m.n}</b> <span class="meta">${m.tier}</span></div>
    <div class="${locked?"warn":"meta"}">${locked?"Nợ "+vnd(o)+". Cần thanh toán trước khi mượn tiếp.":`Đang mượn ${loans.filter(l=>l.m===m.id).length} cuốn, nợ ${vnd(o)}`}</div></div>`;
  const due = addDays(TODAY,+bDays.value);
  document.getElementById("b-rule").textContent = none ? `Đã hết ${b.fmt==="digital"?"giấy phép":"bản"} sẵn có. Có thể đặt giữ chỗ.` :
    `Hạn trả ${fmtD(due)}. ${b.fmt==="digital"?"Quyền đọc tự hết hạn vào ngày này.":"Trễ hạn tính "+vnd(RATE)+" mỗi ngày sau 1 ngày ân hạn."}`;
  document.getElementById("b-do").disabled = locked||none;
  drawSlip(b.isbn, b.fmt==="digital"?"Giấy phép mới":"Bản trên kệ", fresh?{o:TODAY,d:due,isNew:true}:null);
}
bMember.onchange=bBook.onchange=bDays.onchange=()=>renderBorrow();
document.getElementById("b-do").onclick=()=>{
  const b=book(bBook.value), due=addDays(TODAY,+bDays.value);
  loans.push({id:Date.now(),m:bMember.value,isbn:b.isbn,copy:b.fmt==="digital"?"Giấy phép "+(b.out+1):"Bản "+(b.out+1),out:TODAY.toISOString().slice(0,10),due:due.toISOString().slice(0,10)});
  b.out++; renderBorrow(true); fillLoans(); renderAll(false);
  toast(`Đã cho mượn. Hạn trả ${fmtD(due)}.`);
};

/* ---------- catalog / members / fines / notify ---------- */
function renderCatalog(){
  const q = document.getElementById("q").value.toLowerCase(), fm = document.getElementById("fmt").value;
  const rows = books.filter(b=>(!fm||b.fmt===fm)&&(b.t+b.a+b.isbn).toLowerCase().includes(q));
  document.getElementById("cat-body").innerHTML = rows.length ? rows.map(b=>{
    const left=b.copies-b.out;
    const st = left===0?`<span class="st late">Hết, ${b.fmt==="digital"?"có hàng chờ":"đặt giữ chỗ"}</span>`:left<=1?`<span class="st soon">Sắp hết</span>`:`<span class="st ok">Sẵn có</span>`;
    return `<tr><td><div class="title">${b.t}</div><div class="meta">${b.a}</div></td><td class="meta">${b.isbn}</td>
      <td>${b.fmt==="digital"?'<span class="st digital">Điện tử</span>':'<span class="st none">Bản in</span>'}</td>
      <td class="num">${left} / ${b.copies}</td><td>${st}</td></tr>`}).join("")
    : `<tr><td colspan="5" class="meta">Không tìm thấy sách khớp với “${q}”. Thử tìm theo ISBN hoặc tên tác giả.</td></tr>`;
}
document.getElementById("q").oninput=renderCatalog; document.getElementById("fmt").onchange=renderCatalog;

function renderMembers(){
  document.getElementById("mem-body").innerHTML = members.map(m=>{
    const o=owed(m.id), n=loans.filter(l=>l.m===m.id).length, late=loans.some(l=>l.m===m.id&&status(l).k==="late");
    const st = o>=LOCK?'<span class="st late">Tạm khoá mượn</span>':late?'<span class="st soon">Có sách quá hạn</span>':'<span class="st ok">Hoạt động</span>';
    return `<tr><td><div style="display:flex;gap:10px;align-items:center"><div class="avatar">${initials(m.n)}</div><div><b style="font-weight:500">${m.n}</b><div class="meta">${m.id}</div></div></div></td>
      <td>${m.tier}</td><td class="num">${n}</td><td class="num">${vnd(o)}</td><td>${st}</td></tr>`}).join("");
}
function renderFines(){
  const accr = loans.filter(l=>calcFine(l,TODAY).amt>0).map(l=>{const f=calcFine(l,TODAY);return{m:l.m,t:book(l.isbn).t,late:f.late,amt:f.amt,accr:true}});
  document.getElementById("fine-body").innerHTML = [...accr,...fines].map(f=>`<tr><td><div class="title">${f.t}</div><div class="meta">${mem(f.m).n}</div></td>
    <td class="num">${f.late} ngày</td><td class="num">${vnd(f.amt)}</td>
    <td>${f.accr?'<span class="st late">Đang tăng mỗi ngày</span>':f.paid?'<span class="st ok">Đã thu</span>':'<span class="st soon">Chưa thu</span>'}</td></tr>`).join("");
}
function renderNotify(){
  const q = loans.map(l=>{const d=days(TODAY,D(l.due));let k=d===3?"Nhắc trước 3 ngày":d===1?"Nhắc trước 1 ngày":d===-1?"Báo quá hạn":d<=-7?"Gọi điện, khoá thẻ":null;return k&&{l,k}}).filter(Boolean);
  document.getElementById("queue").innerHTML = q.length ? q.map(({l,k})=>`<li><div><b style="font-weight:500">${mem(l.m).n}</b><div class="meta">${book(l.isbn).t}, hạn ${fmtD(D(l.due))}</div></div><div class="right"><span class="st ${k.startsWith("Nhắc")?"soon":"late"}">${k}</span></div></li>`).join("")
    : `<li><div class="meta">Không có lời nhắc nào cần gửi hôm nay.</div></li>`;
  const s=document.getElementById("send"); s.textContent=`Gửi ngay ${q.length} lời nhắc`; s.disabled=!q.length;
  s.onclick=()=>toast(`Đã gửi ${q.length} lời nhắc.`);
}

let tt; function toast(t){const el=document.getElementById("toast");el.textContent=t;el.classList.add("on");clearTimeout(tt);tt=setTimeout(()=>el.classList.remove("on"),2600);}

function renderAll(desk=true){ renderToday(); renderCatalog(); renderMembers(); renderFines(); renderNotify(); if(desk){ mode==="return"?renderReturn():renderBorrow(); } }

fillLoans();
bMember.innerHTML = members.map(m=>`<option value="${m.id}">${m.n}, ${m.id}</option>`).join("");
bBook.innerHTML = books.map(b=>`<option value="${b.isbn}">${b.t} (${b.isbn})</option>`).join("");
renderAll();

/* ---------- login (demo: any non-empty id and password) ---------- */
const loginEl=document.getElementById("login"), appEl=document.getElementById("app");
function showApp(id){ loginEl.hidden=true; appEl.hidden=false; if(id) document.getElementById("who-name").textContent=id; go("today"); }
document.getElementById("login-form").onsubmit=e=>{
  e.preventDefault();
  const id=document.getElementById("l-id").value.trim(), pw=document.getElementById("l-pw").value;
  const err=document.getElementById("l-err");
  if(!id||!pw){ err.hidden=false; (id?document.getElementById("l-pw"):document.getElementById("l-id")).focus(); return; }
  err.hidden=true;
  try{ if(document.getElementById("l-keep").checked) localStorage.setItem("librarian",id); }catch(_){}
  showApp(id);
};
document.getElementById("logout").onclick=()=>{
  try{ localStorage.removeItem("librarian"); }catch(_){}
  appEl.hidden=true; loginEl.hidden=false; document.getElementById("l-pw").value=""; document.getElementById("l-id").focus();
};
try{ const saved=localStorage.getItem("librarian"); if(saved) showApp(saved); }catch(_){}
