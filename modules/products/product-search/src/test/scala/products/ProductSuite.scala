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
		__typename
		... on ElectronicDevice {
			category
		}
		... on Garment {
			category
		}
	}
}
 */

/*
    caliban.CalibanError$ValidationError:
    Filter of InputValue 'filters' of Field 'products' of Object 'Queries' is of kind UNION,
    must be an InputType"
 */
