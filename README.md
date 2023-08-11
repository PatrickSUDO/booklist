# BookList 

This service built with Finagle provides an API to fetch a list of books based on the author's name and, optionally, a publication year. The data is sourced from the New York Times API and is cached for better performance and reduced external API calls.

## Requirements

- Scala 
- SBT 

## Starting the Service

To start the service using SBT:

1. Open a terminal or command prompt.
2. Navigate to the root directory of the project.
3. Run:

```bash
sbt run
```

This will start the service, and it will begin listening for incoming requests.

## Running Tests

This project has unit tests for various components like the cache, the client for the New York Times API, and the main book service.

To run the tests:

1. Open a terminal or command prompt.
2. Go to the root directory of the project.
3. Run:

```bash
sbt test
```

This will execute all the tests, and you'll see the results in the terminal.

## Key Components

- **NYTClient**: This component interacts with the New York Times API to fetch book data.
- **BooksCache**: An in-memory cache that stores book lists for specific queries to reduce the number of external API calls and improve response times.
- **BookService**: The main service that handles incoming requests, interacts with the cache and the NYTClient, and sends back responses.

## Notes

- The service caches responses for 3 minutes by default. This duration is adjustable.
- Ensure you have the necessary API key was set up by environment `NYT_API_KEY` beforehand.


