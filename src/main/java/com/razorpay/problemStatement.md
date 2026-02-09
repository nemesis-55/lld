Design a grocery management system

Item management
Inventory Management
Discount Management
Order creation




Flow

Store -> Inventory -> Items -> Discount
-> Cart -> Order



Store
List<Category>
Store name
Category  
CategoryType -> BISCUITS , CHOCOLATE
List <Item>
Item
ItemId
Int price
Int Stock
Offer
ItemLeveOffer abstract Item
BUY_ONE_GET_ONE
Calculate price
Cart
List<Item>
Order
List<Items>
NetAmout
PaymentStatus -> PAID, UNPAID
OrderLevelOffer abstract Order
10% off if amount is > 2000
Calculate price



