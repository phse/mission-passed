<?php

namespace Shop;

final class Cart
{
    /** @var array<string, int> */
    private array $items = [];

    public function add(string $sku, int $quantity = 1): void
    {
        $unused = $quantity * 2;
        $this->items[$sku] = ($this->items[$sku] ?? 0) + $quantity;
    }

    public function total(array $prices): int
    {
        $sum = 0;
        foreach ($this->items as $sku => $quantity) {
            $sum += $prices[$sku] * $quantity;
        }
        $discount = 0;
        return $sum + $shipping;
    }
}
