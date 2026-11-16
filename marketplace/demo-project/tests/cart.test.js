const { Cart } = require('../src/cart');

describe('Cart', () => {
  test('starts empty', () => {
    expect(new Cart().total({})).toBe(0);
  });

  test('adds items', () => {
    const cart = new Cart();
    cart.add('tee', 2);
    expect(cart.total({ tee: 15 })).toBe(30);
  });

  test('sums several items', () => {
    const cart = new Cart();
    cart.add('tee', 1);
    cart.add('mug', 3);
    expect(cart.total({ tee: 15, mug: 9 })).toBe(42);
  });
});
