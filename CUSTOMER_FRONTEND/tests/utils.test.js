import { test } from 'node:test'
import assert from 'node:assert/strict'
import { money,discountPercent,hasDiscount } from '../src/utils/money.js'
import { imageUrl } from '../src/utils/image.js'
import { readQuery,ids,priceError } from '../src/utils/query.js'
test('canonical discount fields and clean decimal format',()=>{assert.equal(discountPercent('20.00'),'20');assert.equal(discountPercent('12.50'),'12.5');for(const value of [null,0,-2,'bad',Infinity,101])assert.equal(discountPercent(value),'');assert.equal(hasDiscount({dangGiamGia:true,phanTramGiam:20}),true);assert.equal(hasDiscount({dangGiamGia:false,phanTramGiam:20}),false);assert.equal(hasDiscount({dangGiamGia:true,phanTramGiam:0}),false)})
test('VND zero valid, missing value not fabricated',()=>{assert.ok(money(1000000).includes('1.000.000'));assert.ok(money(0).includes('0'));assert.equal(money(null),'—');assert.equal(money('invalid'),'—')})
test('image URL permits real relative and http data only',()=>{assert.equal(imageUrl('/uploads/products/real.png'),'/uploads/products/real.png');assert.equal(imageUrl('https://example.invalid/real.png'),'https://example.invalid/real.png');for(const v of ['javascript:alert(1)','//example.invalid/x','data:image/svg+xml,bad',null,''])assert.equal(imageUrl(v),'')})
test('query parsing handles repeated IDs and rejects NaN/undefined',()=>{assert.deepEqual(ids(['1','1','2','bad','-1']),[1,2]);const q=readQuery({page:'2',brandId:['1','2'],sort:'priceAsc',minPrice:'1000'});assert.equal(q.page,1);assert.equal(q.size,12);assert.deepEqual(q.brandId,[1,2]);assert.equal(q.minPrice,'1000');assert.equal(readQuery({page:'NaN',sort:'bad'}).page,0);assert.equal(readQuery({sort:'bad'}).sort,'newest')})
test('price bounds validate without inventing campaign rules',()=>{assert.equal(priceError('',''),'');assert.equal(priceError('0','1000'),'');assert.notEqual(priceError('-1','100'),'');assert.notEqual(priceError('200','100'),'');assert.notEqual(priceError('NaN','100'),'')})

test('page size is bounded and supports real pagination',()=>{assert.equal(readQuery({size:'6'}).size,6);assert.equal(readQuery({size:'24'}).size,24);assert.equal(readQuery({size:'1000'}).size,12)})
