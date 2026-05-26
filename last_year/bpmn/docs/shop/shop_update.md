# Update Shop <!-- omit in toc -->

![Update Shop](./assets/ShopUpdate.png)

<details>
<summary>Table of Contents</summary>

- [Shop - Initiator: Decide the Data for Shop Update order](#shop---initiator-decide-the-data-for-shop-update-order)
- [LAAS - Executor: Verify if execute product is possible](#laas---executor-verify-if-execute-product-is-possible)
- [Shop - Initiator: Check Shop Update order](#shop---initiator-check-shop-update-order)
- [Shop - Initiator: Decide what to do next](#shop---initiator-decide-what-to-do-next)

</details>

## Shop - Initiator: Decide the Data for Shop Update order

The first task to update a shop, it is necessary to provide the following information:

- **ShopID**: The ID of the shop to be updated, an integer value.
- **LocationAddress**: The address of the shop, a string value.
- **LocationPostalCode**: The postal code of the shop's location, a string value.
- **Name**: The name of the shop, a string value.

## LAAS - Executor: Verify if execute product is possible

The executor will verify if the provided data is valid and if the shop can be updated.

If the executer considers the product update possible, it will proceed to update the shop and will return to the initiator in the ["Check Shop Update order"](#shop---initiator-check-shop-update-order) task.

If not, it goes to the ["Decide what to do next"](#shop---initiator-decide-what-to-do-next) task.

## Shop - Initiator: Check Shop Update order

After the shop is updated, the initiator must acknowledge that you have received confirmation of the update.

## Shop - Initiator: Decide what to do next

If the executer said that the shop update is not possible, the initiator can choose to make or not a new request with the same data.

If it chooses to make a new request, it will return to the ["Verify if execute product is possible"](#laas---executor-verify-if-execute-product-is-possible) task.

If not, the process will end.
