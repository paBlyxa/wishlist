# Wishlist

Wishlist is a service that help you organization your desires.
The author may provide access to wishlist to family, friends, and other people.
Then, they can reserve a gift or divide the cost your bigger dreams.

## Getting Started

### Prerequisites

You need to install:

- JDK 11
- SBT
- Scala  
- Docker

### Installing

To install JDK, SBT and Scala follow this:

```
https://docs.scala-lang.org/getting-started/index.html
```

And for Docker

```
https://www.docker.com/get-started
```

### Tests

This example project contains unit tests, which mock the repository that accesses the database.

### Built

Build with sbt-native-packager

```
sbt docker:publishLocal
```

### Running

You can run application using docker. By default it listens on port 8080.

```
docker compose up
```

```
Simple frontend is available by url: /index.html

Swagger API documentation is available by url: /swagger/docs/
```

## Features

- Create an online private or public wishlists
- Add items to list
- Friends can reserve items or divide cost


## Endpoints

There are some endpoint's examples, see more in swagger API documentation.

Method | Url                | Description
------ | ------------------ | -----------
GET    | /user/{username}   | Returns the user for the specified username.
POST   | /user              | Creates a user, give as body JSON with the description, return the created user.
GET    | /{userId}/wishlist/{wishlistId} | Returns the wishlist for the specified wishlistId.
POST   | /{userId}/wishlist | Creates a wishlist, give as body JSON with the description, returns the created wishlist.
PATCH  | /{userId}/wishlist/{wishlistId} | Updates an existing wishlist, give as body JSON with the description, returns the updated wishlist when a wishlist is present with the specified wishlistId.
DELETE | /{userId}/wishlist/{wishlistId} | Deletes the wishlist with the specified wishlistId.
POST   | /{userId}/wishlist/{wishlistId}/wish | Creates a wish in the wishlist for the specified wishlistId, give as body JSON with the description, returns the created wish.
GET    | /{userId}/wishlist/{wishlistId}/{wishId} | Returns the wish for the specified wishId.
DELETE | /{userId}/wishlist/{wishlistId}/{wishId} | Deletes the wish with the specified wishId.
PATCH  | /{userId}/wishlist/{wishlistId}/wish/{wishId} | Updates an existing wish, give as body JSON with the description, returns the updated wish when a wish is present with the specified wishId.
DELETE | /{userId}/wishlist/{wishlistId}/wishes | Deletes all wishes with the specified wishlistId.
GET    | /{userId}/wishlist/{wishlistId}/list | Returns the wishlist's list with matching filter (by username or/and name, order by username, name, createAt).
GET    | /{userId}/wishlist/{wishlistId}/wishes | Returns all wishes with the specified wishlistId.
PATCH  | /{userId}/wishlist/{wishlistId}/wish/{wishId}?status=<Status> | Updates status of existing wish (reserve), returns the updated wish when a wish is present with the specified wishId.

## Acknowledgments

* My deepest gratitude to DINS Scala School

