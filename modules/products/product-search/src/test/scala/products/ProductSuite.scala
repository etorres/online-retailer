package es.eriktorr
package products

import products.Product.Queries

import caliban.*
import munit.FunSuite

final class ProductSuite extends FunSuite:
  test("should work"):
    println(render[Queries])
    fail("not implemented")

/*
query GetProductCategory {
	product(id: 100) {
		category
		__typename
		... on ElectronicDevice {
			powerConsumption
		}
		... on Garment {
			color
			size
		}
	}
}
 */
