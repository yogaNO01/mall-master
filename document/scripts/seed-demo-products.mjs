import { writeFile } from 'node:fs/promises'

const base = process.env.MALL_ADMIN_URL || 'http://127.0.0.1:8080'
const portal = process.env.MALL_PORTAL_URL || 'http://127.0.0.1:8085'
const assets = process.env.MALL_ASSET_URL || 'http://192.168.1.10:5174/static/home'
const password = process.env.MALL_ADMIN_PASSWORD
if (!password) throw new Error('Set MALL_ADMIN_PASSWORD before running this script.')
if (!['127.0.0.1', 'localhost'].includes(new URL(base).hostname)) throw new Error('This script seeds the local development server only.')
let token = ''
const report = []
const reportPath = process.env.MALL_SEED_REPORT || 'demo-products-result.json'

async function api(path, body) {
  const response = await fetch(`${base}${path}`, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: token } : {}) },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
  const result = await response.json()
  if (!response.ok || result.code !== 200) throw new Error(`${path}: ${result.message || response.status}`)
  return result.data
}

const groups = [
  { category: '衣服', code: 'SHIRT', image: '138da.png', unit: '件', weight: 260, colors: ['浅蓝色', '白色'], sizes: ['S', 'M', 'L'], material: '棉混纺面料（示例参数）', care: '建议轻柔水洗，避免高温烘干。',
    items: [['日常通勤衬衫', 109, 159], ['宽松长袖衬衫', 129, 189], ['简约休闲衬衫', 139, 199]] },
  { category: '包包', code: 'BAG', image: '8df2b.png', unit: '个', weight: 680, colors: ['棕色', '黑色'], sizes: ['均码'], material: '合成革面料（示例参数）', care: '使用软布擦拭，避免长时间接触水和暴晒。',
    items: [['通勤大容量托特包', 179, 259], ['简约肩背托特包', 199, 289], ['日常休闲手提包', 219, 329]] },
  { category: '鞋子', code: 'SHOE', image: '4f09c.png', unit: '双', weight: 620, colors: ['米白色', '深蓝色'], sizes: ['38', '39', '40', '41', '42'], material: '织物与合成革鞋面，橡胶鞋底（示例参数）', care: '建议使用软毛刷轻柔清洁，避免浸泡和高温烘干。',
    items: [['轻便休闲运动鞋', 179, 259], ['复古系带休闲鞋', 199, 289], ['日常通勤板鞋', 229, 329]] },
  { category: '墨镜', code: 'SUN', image: '8fcba.png', unit: '副', weight: 35, colors: ['玳瑁色', '黑色'], sizes: ['均码'], material: '树脂镜片与塑料镜框（示例参数）', care: '使用镜布轻柔擦拭，收纳时放入眼镜盒。',
    items: [['复古圆框太阳镜', 79, 129], ['轻量休闲太阳镜', 99, 159], ['日常出行太阳镜', 119, 189]] },
]

const login = await api('/admin/login', { username: process.env.MALL_ADMIN_USERNAME || 'admin', password })
token = login.tokenHead + login.token
const categories = await api('/productCategory/list/withChildren')
const attrCategories = (await api('/productAttribute/category/list?pageNum=1&pageSize=1000')).list
for (const group of groups) {
  group.categoryId = categories.find(item => item.name === group.category)?.id
  group.attributeCategoryId = attrCategories.find(item => item.name === group.category)?.id
  if (!group.categoryId || !group.attributeCategoryId) throw new Error(`Missing category or attribute category: ${group.category}`)
  const image = await fetch(`${assets}/${group.image}`)
  if (!image.ok) throw new Error(`Missing product image: ${group.image}`)
  await image.arrayBuffer()
}

let brands = (await api('/brand/list?pageNum=1&pageSize=1000')).list
if (!brands.some(item => item.name === '示例精选')) {
  await api('/brand/create', {
    name: '示例精选', firstLetter: 'S', sort: 0, factoryStatus: 0, showStatus: 1,
    logo: `${assets}/4f09c.png`, bigPic: `${assets}/b16cc.png`,
    brandStory: '用于本地商城页面演示与功能测试的示例品牌。商品图片、规格、价格和库存仅供演示，不代表实际在售商品。',
  })
  brands = (await api('/brand/list?pageNum=1&pageSize=1000')).list
}
const brand = brands.find(item => item.name === '示例精选')

for (const group of groups) {
  const specs = [
    { name: '颜色', type: 0, options: group.colors },
    { name: '尺码', type: 0, options: group.sizes },
    { name: '材质', type: 1, options: [] },
    { name: '保养说明', type: 1, options: [] },
  ]
  for (const spec of specs) {
    let list = (await api(`/productAttribute/list/${group.attributeCategoryId}?type=${spec.type}&pageNum=1&pageSize=1000`)).list
    if (!list.some(item => item.name === spec.name)) {
      await api('/productAttribute/create', {
        productAttributeCategoryId: group.attributeCategoryId, name: spec.name, type: spec.type,
        selectType: spec.type === 0 ? 2 : 0, inputType: spec.type === 0 ? 1 : 0,
        inputList: spec.options.join(','), sort: 0, filterType: spec.name === '颜色' ? 1 : 0,
        searchType: 0, relatedStatus: 0, handAddStatus: spec.type === 0 ? 1 : 0,
      })
      list = (await api(`/productAttribute/list/${group.attributeCategoryId}?type=${spec.type}&pageNum=1&pageSize=1000`)).list
    }
    spec.id = list.find(item => item.name === spec.name).id
  }
  for (let index = 0; index < group.items.length; index++) {
    const [title, price, originalPrice] = group.items[index]
    const sn = `DEMO-${group.code}-${String(index + 1).padStart(3, '0')}`
    let products = (await api(`/product/list?productSn=${encodeURIComponent(sn)}&pageNum=1&pageSize=1000`)).list
    let product = products.find(item => item.productSn === sn)
    const created = !product
    if (!product) {
      const skuStockList = group.colors.flatMap(color => group.sizes.map(size => ({
        skuCode: `${sn}-${group.colors.indexOf(color) + 1}-${size}`, price, stock: 30, lowStock: 5,
        sale: 0, lockStock: 0, pic: `${assets}/${group.image}`,
        spData: JSON.stringify([{ key: '颜色', value: color }, { key: '尺码', value: size }]),
      })))
      const description = `${title}，适合日常穿搭与出行。此商品为本地演示数据，图片及参数仅供测试，不代表实际销售承诺。`
      const html = `<h2>${title}</h2><p>${description}</p><img src="${assets}/${group.image}" alt="${title}" style="max-width:100%;height:auto"/><h3>商品参数</h3><p>货号：${sn}</p><p>材质：${group.material}</p><p>可选颜色：${group.colors.join('、')}</p><p>可选尺码：${group.sizes.join('、')}</p><h3>保养建议</h3><p>${group.care}</p>`
      await api('/product/create', {
        name: `【示例】${title}`, subTitle: '本地演示商品 · 支持颜色与尺码选择', description,
        productSn: sn, brandId: brand.id, brandName: brand.name,
        productCategoryId: group.categoryId, productCategoryName: group.category,
        productAttributeCategoryId: group.attributeCategoryId,
        pic: `${assets}/${group.image}`, albumPics: '', price, originalPrice,
        stock: skuStockList.reduce((sum, sku) => sum + sku.stock, 0), lowStock: 5,
        unit: group.unit, weight: group.weight, sort: 0, sale: 0,
        deleteStatus: 0, publishStatus: 1, verifyStatus: 1, newStatus: 1, recommandStatus: 0,
        previewStatus: 0, promotionType: 0, promotionPerLimit: 0, feightTemplateId: 0,
        giftPoint: 0, giftGrowth: 0, usePointLimit: 0, serviceIds: '', keywords: `${group.category},示例商品`,
        note: '自动创建的本地示例商品，可按货号 DEMO- 搜索。请按实际商品资料修改后再使用。',
        detailTitle: title, detailDesc: description, detailHtml: html, detailMobileHtml: html,
        skuStockList,
        productAttributeValueList: specs.map(spec => ({ productAttributeId: spec.id,
          value: spec.name === '材质' ? group.material : spec.name === '保养说明' ? group.care : spec.options.join(',') })),
        memberPriceList: [], productLadderList: [], productFullReductionList: [],
        subjectProductRelationList: [], prefrenceAreaProductRelationList: [],
      })
      products = (await api(`/product/list?productSn=${encodeURIComponent(sn)}&pageNum=1&pageSize=1000`)).list
      product = products.find(item => item.productSn === sn)
    }
    if (!product) throw new Error(`Created product not found: ${sn}`)
    const detail = await api(`/product/updateInfo/${product.id}`)
    if (!detail.skuStockList?.length || !detail.productAttributeValueList?.length) throw new Error(`Incomplete product: ${sn}`)
    const storefront = await fetch(`${portal}/product/detail/${product.id}`).then(response => response.json())
    if (storefront.code !== 200 || storefront.data?.product?.productSn !== sn) throw new Error(`Storefront cannot read: ${sn}`)
    report.push({ id: product.id, name: product.name, category: group.category, productSn: sn, price,
      stock: product.stock, skuCount: detail.skuStockList.length, created })
    await writeFile(reportPath, JSON.stringify(report, null, 2) + '\n')
    console.log(`${created ? 'Created' : 'Exists'} ${product.id}: ${product.name} (${group.category}, ${detail.skuStockList.length} SKUs)`)
  }
}
console.log(`Verified ${report.length} demo products; created ${report.filter(item => item.created).length}. Report: ${reportPath}`)
