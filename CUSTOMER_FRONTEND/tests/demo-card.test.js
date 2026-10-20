import test from 'node:test'
import assert from 'node:assert/strict'
import { DEMO_CARDS, DEMO_HOLDER, DEMO_ISSUE_DATE, demoCardError, demoCardResult } from '../src/utils/demoCard.js'
const card = (number = DEMO_CARDS[0].number) => ({ number, holder: DEMO_HOLDER, issueDate: DEMO_ISSUE_DATE })
test('public NCB success card and its formatting are accepted', () => {
  assert.equal(demoCardError(card()), '')
  assert.equal(demoCardResult(card('9704 1985 2619 1432 198')), 'OK')
  assert.equal(demoCardError({ ...card(), holder: ' nguyen  van a ' }), '')
})
test('only listed test cards, exact holder and issue date are accepted', () => {
  for (const number of ['', '4111111111111111', '9704198526191432198<script>', '9704-1985-2619-1432-198']) assert.ok(demoCardError(card(number)))
  assert.ok(demoCardError({ ...card(), holder: 'NGUYEN VAN B' }))
  for (const date of ['15/07', '07/2015', '7/15', '13/15', '']) assert.ok(demoCardError({ ...card(), issueDate: date }))
})
test('all four public failure cards produce a failure outcome', () => {
  for (const sample of DEMO_CARDS.slice(1)) { assert.equal(demoCardError(card(sample.number)), ''); assert.notEqual(demoCardResult(card(sample.number)), 'OK') }
})
