import { mkdir, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { resolve } from 'node:path'

const workspace = fileURLToPath(new URL('../../..', import.meta.url))
const base = process.env.MALL_ADMIN_URL || 'http://127.0.0.1:8080'
const assetBase = process.env.MALL_ASSET_URL || 'http://192.168.1.10:5174/static'
if (!['localhost', '127.0.0.1'].includes(new URL(base).hostname)) throw Error('Local catalog only')
if (!process.env.MALL_ADMIN_PASSWORD) throw Error('Set MALL_ADMIN_PASSWORD')
let token = ''
async function api(path, body, method = body === undefined ? 'GET' : 'POST') {
  const response = await fetch(base + path, { method, headers: { 'Content-Type':'application/json', ...(token ? {Authorization:token} : {}) }, ...(body === undefined ? {} : {body:JSON.stringify(body)}) })
  const result = await response.json()
  if (!response.ok || result.code !== 200) throw Error(`${path}: ${result.message || response.status}`)
  return result.data
}
const list = async path => {
  const values = []
  for (let pageNum = 1; ; pageNum++) {
    const page = await api(`${path}${path.includes('?') ? '&' : '?'}pageNum=${pageNum}&pageSize=100`)
    values.push(...page.list)
    if (pageNum >= page.totalPage || !page.list.length) return values
  }
}
const login = await api('/admin/login', { username:process.env.MALL_ADMIN_USERNAME || 'admin', password:process.env.MALL_ADMIN_PASSWORD })
token = login.tokenHead + login.token
const brandsBefore = await list('/brand/list')
const categoriesBefore = await list('/productCategory/list/0')
const attrCategoriesBefore = await list('/productAttribute/category/list')
const productsBefore = await list('/product/list')
const attributesBefore = []
for (const category of attrCategoriesBefore) for (const type of [0,1]) attributesBefore.push(...await list(`/productAttribute/list/${category.id}?type=${type}`))
const detailsBefore = []
for (const product of productsBefore) detailsBefore.push(await api(`/product/updateInfo/${product.id}`))
const homeBrandsBefore = await list('/home/brand/list')
const homeProductsBefore = await list('/home/newProduct/list')
const backupDir = resolve(workspace, '.local-tools/catalog-backups')
await mkdir(backupDir, { recursive:true })
const backupFile = resolve(backupDir, `catalog-${new Date().toISOString().replace(/[:.]/g,'-')}.json`)
await writeFile(backupFile, JSON.stringify({brands:brandsBefore,categories:categoriesBefore,attributeCategories:attrCategoriesBefore,attributes:attributesBefore,products:detailsBefore,homeBrands:homeBrandsBefore,homeProducts:homeProductsBefore},null,2))
console.log(`Backup: ${backupFile}`)

const brandDefinitions = [
  ['爱马仕','hermes','HERMÈS','PARIS','H'],['香奈儿','chanel','CHANEL','PARIS','C'],
  ['迪奥','dior','Dior','PARIS','D'],['普拉达','prada','PRADA','MILANO','P'],
  ['芬迪','fendi','FENDI','ROMA','F'],['罗意威','loewe','LOEWE','MADRID','L'],['圣罗兰','saint-laurent','SAINT LAURENT','PARIS','S'],
]
async function asset(name, content) {
  for (const root of ['mall-app-web-master/src/static/catalog','mall-admin-web-master/public/catalog']) {
    await mkdir(resolve(workspace,root),{recursive:true})
    await writeFile(resolve(workspace,root,name),content)
  }
  return `${assetBase}/catalog/${name}`
}
const productSvg = drawing => `<svg xmlns="http://www.w3.org/2000/svg" width="830" height="750" viewBox="0 0 415 375"><defs><linearGradient id="gold"><stop stop-color="#9b7430"/><stop offset=".4" stop-color="#f4dd93"/><stop offset=".7" stop-color="#c4a15d"/><stop offset="1" stop-color="#87632e"/></linearGradient><radialGradient id="bead"><stop stop-color="#d5deee"/><stop offset=".6" stop-color="#768bb1"/><stop offset="1" stop-color="#3c547f"/></radialGradient></defs><rect width="415" height="375" fill="#f8f6f2"/><ellipse cx="207" cy="299" rx="128" ry="14" fill="#dfd9cd" opacity=".5"/>${drawing}</svg>`
const jewelImage = await asset('jewelry.svg',productSvg('<ellipse cx="177" cy="194" rx="64" ry="81" fill="none" stroke="url(#gold)" stroke-width="17" transform="rotate(-24 177 194)"/><ellipse cx="254" cy="214" rx="56" ry="70" fill="none" stroke="url(#gold)" stroke-width="13" transform="rotate(32 254 214)"/><path d="M153 113l17-23 28 6 11 28-32 22z" fill="#dce6f0" stroke="#b6a16f" stroke-width="3"/><path d="M153 113l56 11-39-34 7 56z" fill="none" stroke="#fff" stroke-width="2"/>'))
const crystalImage = await asset('crystal.svg',productSvg(Array.from({length:17},(_,i)=>{const a=i/17*Math.PI*2;return `<circle cx="${207+Math.cos(a)*103}" cy="${191+Math.sin(a)*76}" r="17" fill="url(#bead)" stroke="#94a4bf" stroke-width="1"/>`}).join('')+'<circle cx="207" cy="270" r="19" fill="url(#gold)"/><path d="M207 256l4 9 10 1-8 6 2 10-8-5-8 5 2-10-8-6 10-1z" fill="#f7e8bc"/>'))
const zodiac = ['白羊座','金牛座','双子座','巨蟹座','狮子座','处女座','天秤座','天蝎座','射手座','摩羯座','水瓶座','双鱼座']
const groups = [
  {name:'包包',code:'BAG',image:`${assetBase}/home/8df2b.png`,unit:'个',sizes:['小号','中号'],colors:['棕色|#875536','黑色|#252525','米白色|#F2EDE3'],price:289,material:'合成革面料（示例）',style:'简约通勤托特包',weight:680},
  {name:'鞋子',code:'SHOE',image:`${assetBase}/home/4f09c.png`,unit:'双',sizes:['38','39','40','41','42'],colors:['米白色|#F2EDE3','深蓝色|#30486D','黑色|#252525'],price:259,material:'织物、合成革鞋面，橡胶鞋底（示例）',style:'轻便系带休闲鞋',weight:620},
  {name:'衣服',code:'SHIRT',image:`${assetBase}/home/138da.png`,unit:'件',sizes:['S','M','L','XL'],colors:['浅蓝色|#B8D5ED','白色|#FFFFFF','黑色|#252525'],price:159,material:'棉混纺面料（示例）',style:'长袖休闲衬衫',weight:260},
  {name:'墨镜',code:'SUN',image:`${assetBase}/home/8fcba.png`,unit:'副',sizes:['标准款'],colors:['玳瑁色|#78533B','黑色|#252525','棕色|#875536'],price:129,material:'树脂镜片与塑料镜框（示例）',style:'复古圆框太阳镜',weight:35},
  {name:'珠宝',code:'JEWEL',image:jewelImage,unit:'件',sizes:['13号','15号','17号'],colors:['金色|#C7A45B','银色|#C8CED4'],price:199,material:'合金与装饰石（示例）',style:'简约叠戴饰品',weight:20},
  {name:'星座水晶手链',code:'CRYSTAL',image:crystalImage,unit:'条',sizes:['14cm','16cm','18cm'],colors:['蓝色|#5C8CAE','紫色|#AC92CA'],price:169,material:'装饰晶石与弹力线（示例）',style:'星座晶石手链',weight:45},
]

const brands = []
for (const [index,[name,code,word,city,firstLetter]] of brandDefinitions.entries()) {
  const logo = await asset(`brand-${code}.svg`,`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 480 260"><rect width="480" height="260" fill="#f6f7f8"/><text x="240" y="136" text-anchor="middle" fill="#153451" font-family="${code==='dior' ? 'Georgia,serif' : 'Arial,sans-serif'}" font-size="${word.length>10?30:46}" font-weight="${code==='dior'?400:700}" letter-spacing="3">${word}</text><text x="240" y="174" text-anchor="middle" fill="#81909d" font-family="Arial,sans-serif" font-size="12" letter-spacing="5">${city}</text></svg>`)
  const payload = {name,firstLetter,sort:100-index,factoryStatus:0,showStatus:1,logo,bigPic:`${assetBase}/home/b16cc.png`,brandStory:`发现${name}精选风格，探索包包、鞋履、服饰、墨镜及珠宝配饰。这里的商品为本地演示资料，可在品牌管理和商品管理中继续完善。`}
  const existing = brandsBefore.find(item=>item.name===name)
  await api(existing ? `/brand/update/${existing.id}` : '/brand/create',payload)
  const saved = (await list(`/brand/list?keyword=${encodeURIComponent(name)}`)).find(item=>item.name===name)
  brands.push({...saved,code})
}

const result = []
for (const [groupIndex,group] of groups.entries()) {
  let category = categoriesBefore.find(item=>item.name===group.name)
  if (!category) {
    await api('/productCategory/create',{parentId:0,name:group.name,productUnit:group.unit,navStatus:1,showStatus:1,sort:100-groupIndex,productAttributeIdList:[]})
    category=(await list('/productCategory/list/0')).find(item=>item.name===group.name)
  }
  let attrCategory=attrCategoriesBefore.find(item=>item.name===group.name)
  if (!attrCategory) {
    await api(`/productAttribute/category/create?name=${encodeURIComponent(group.name)}`,{})
    attrCategory=(await list('/productAttribute/category/list')).find(item=>item.name===group.name)
  }
  const definitions=[{name:'颜色',type:0,options:group.colors},{name:'尺码',type:0,options:group.sizes},...(group.code==='CRYSTAL'?[{name:'星座',type:0,options:zodiac}]:[]),{name:'材质',type:1,options:[]},{name:'保养说明',type:1,options:[]},{name:'品质等级',type:1,options:['高级','中级','基础级']}]
  for (const [index,definition] of definitions.entries()) {
    const existing=(await list(`/productAttribute/list/${attrCategory.id}?type=${definition.type}`)).find(item=>item.name===definition.name)
    const payload={productAttributeCategoryId:attrCategory.id,name:definition.name,type:definition.type,selectType:definition.type===0?2:0,inputType:definition.options.length?1:0,inputList:definition.options.join(','),sort:100-index,filterType:definition.name==='颜色'?1:0,searchType:1,relatedStatus:0,handAddStatus:definition.type===0?1:0}
    await api(existing?`/productAttribute/update/${existing.id}`:'/productAttribute/create',payload)
    definition.id=existing?.id || (await list(`/productAttribute/list/${attrCategory.id}?type=${definition.type}`)).find(item=>item.name===definition.name).id
  }
  // Retire obsolete sales dimensions without dropping their historical values.
  const extraSpecs=(await list(`/productAttribute/list/${attrCategory.id}?type=0`)).filter(item=>!definitions.some(def=>def.type===0&&def.id===item.id))
  for (const extra of extraSpecs) await api(`/productAttribute/update/${extra.id}`,{...extra,type:1})
  await api(`/productCategory/update/${category.id}`,{parentId:0,name:group.name,productUnit:group.unit,navStatus:1,showStatus:1,sort:100-groupIndex,icon:group.image,keywords:group.name,description:`${group.name}精选，支持品牌、颜色和尺码筛选。`,productAttributeIdList:definitions.filter(item=>item.type===0).map(item=>item.id)})
  for (const [index,brand] of brands.entries()) {
    const sn=`CAT-${group.code}-${brand.code.toUpperCase()}`
    const existing=productsBefore.find(item=>item.productSn===sn) || productsBefore.find(item=>item.productSn===`DEMO-${group.code}-${String(index+1).padStart(3,'0')}`)
    const price=group.price+index*20
    let combinations=[[]]
    for (const spec of definitions.filter(item=>item.type===0)) combinations=combinations.flatMap(items=>spec.options.map(value=>[...items,{key:spec.name,value}]))
    const skuStockList=combinations.map((specs,skuIndex)=>({skuCode:`${sn}-${String(skuIndex+1).padStart(3,'0')}`,spData:JSON.stringify(specs),price,stock:12,lowStock:3,sale:0,lockStock:0,pic:group.image}))
    const description=`${group.style}，适合日常搭配。示例商品用于页面展示与规格选择测试，具体商品资料可在后台编辑。`
    const quality=['高级','中级','基础级'][index%3]
    const care=group.code==='SHIRT'?'建议轻柔水洗，避免高温烘干。':'建议使用柔软干布清洁，避免浸泡、暴晒和接触化学清洁剂。'
    const html=`<h2>${brand.name} · ${group.style}</h2><p>${description}</p><img src="${group.image}" style="max-width:100%;height:auto" alt="${group.style}"/><h3>商品信息</h3><p>货号：${sn}</p><p>品牌：${brand.name}</p><p>材质：${group.material}</p><p>尺码：${group.sizes.join('、')}</p><h3>保养建议</h3><p>${care}</p>`
    const payload={name:`【示例】${brand.name}${group.style}`,subTitle:'多色可选 · 多规格选择',description,productSn:sn,brandId:brand.id,brandName:brand.name,productCategoryId:category.id,productCategoryName:group.name,productAttributeCategoryId:attrCategory.id,pic:group.image,albumPics:'',price,originalPrice:price+80,stock:skuStockList.length*12,lowStock:3,unit:group.unit,weight:group.weight,sort:100-groupIndex,sale:0,deleteStatus:0,publishStatus:1,verifyStatus:1,newStatus:1,recommandStatus:1,previewStatus:0,promotionType:0,promotionPerLimit:0,feightTemplateId:0,giftPoint:0,giftGrowth:0,usePointLimit:0,serviceIds:'',keywords:`${group.name},${brand.name}`,note:'本地示例资料，颜色以色值保存；不代表品牌实际在售款式。',detailTitle:group.style,detailDesc:description,detailHtml:html,detailMobileHtml:html,skuStockList,productAttributeValueList:definitions.map(def=>({productAttributeId:def.id,value:def.type===0?def.options.join(','):def.name==='材质'?group.material:def.name==='保养说明'?care:quality})),memberPriceList:[],productLadderList:[],productFullReductionList:[],subjectProductRelationList:[],prefrenceAreaProductRelationList:[]}
    await api(existing?`/product/update/${existing.id}`:'/product/create',payload)
    const saved=(await list(`/product/list?productSn=${encodeURIComponent(sn)}`)).find(item=>item.productSn===sn)
    const detail=await api(`/product/updateInfo/${saved.id}`)
    if (detail.skuStockList.length!==skuStockList.length || detail.skuStockList.reduce((sum,sku)=>sum+sku.stock,0)!==saved.stock) throw Error(`Invalid SKU inventory: ${sn}`)
    result.push({id:saved.id,name:saved.name,category:group.name,brand:brand.name,productSn:sn,skuCount:detail.skuStockList.length})
  }
  console.log(`${group.name}: 7 products, category ${category.id}, attributes ${attrCategory.id}`)
}
// Keep unrelated old goods in management history, off the storefront.
for (const product of productsBefore) if (!result.some(item=>item.id===product.id)&&!groups.some(group=>group.name===product.productCategoryName)) await api(`/product/update/publishStatus?ids=${product.id}&publishStatus=0`,{})
for (const brand of brandsBefore) if (!brands.some(item=>item.id===brand.id)) {
  if (brand.name==='示例精选') await api(`/brand/delete/${brand.id}`,undefined,'GET')
  else await api(`/brand/update/showStatus?ids=${brand.id}&showStatus=0`,{})
}
for (const row of homeBrandsBefore) if (!brands.some(brand=>brand.id===row.brandId)) await api(`/home/brand/delete?ids=${row.id}`,{})
const currentHomeBrands=await list('/home/brand/list')
const missingBrands=brands.filter(brand=>!currentHomeBrands.some(row=>row.brandId===brand.id))
if (missingBrands.length) await api('/home/brand/create',missingBrands.map(brand=>({brandId:brand.id,brandName:brand.name})))
for (const row of homeProductsBefore) if (!result.some(product=>product.id===row.productId)) await api(`/home/newProduct/delete?ids=${row.id}`,{})
const currentHomeProducts=await list('/home/newProduct/list')
const homeSelection=[...result.filter(item=>['衣服','鞋子','包包','墨镜'].includes(item.category)&&item.brand==='爱马仕'),...result.filter(item=>['衣服','鞋子','包包','墨镜'].includes(item.category)&&item.brand==='香奈儿')]
const missingProducts=homeSelection.filter(product=>!currentHomeProducts.some(row=>row.productId===product.id))
if (missingProducts.length) await api('/home/newProduct/create',missingProducts.map(product=>({productId:product.id,productName:product.name})))
const reportFile=resolve(workspace,'.local-tools/organized-catalog-result.json')
await writeFile(reportFile,JSON.stringify({backupFile,brands:brands.map(({id,name})=>({id,name})),products:result},null,2))
console.log(`Completed: ${brands.length} brands, ${groups.length} categories, ${result.length} products. Report: ${reportFile}`)
