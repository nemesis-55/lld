1.

register_user(“Pralove”, “M”, “phoneNumber-1”, “560035”)

register_user(“Nitesh”, “M”, “phoneNumber-2”, “560088”)

register_user(“Vatsal”, “M”,  “phoneNumber-3”, “560088”)



2.

register_restaurant(“Food Court-1”, “560088,560035”, “NI Thali”, 100)



register_restaurant(“Food Court-2”, “560088”, “Burger”, 120)



register_restaurant(“Food Court-3”, “560035”, “SI Thali”, 150)





4

show_restaurant(“user1”, “price”)



Output :  Food Court-2, Burger

Food Court-1, NI Thali

//only two restaurant serviceable for user i.e. 560088



3.

create_review(“user1”, “Food Court-2”, 3, “Good Food”)

create_review(“user2”, “Food Court-1”, 5, “Nice Food”)



4.

show_restaurant(“user1”, “rating”)

Output :  Food Court-1, NI Thali

Food Court-2, Burger



5.

show_rating("Food Court-3")

Output: 3, “Good Food”