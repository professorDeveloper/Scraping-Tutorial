
### Setup

In build.gradle repositories:

```groovy
maven { url 'https://jitpack.io' }
```

In app/build.gradle dependencies:

```groovy
implementation 'com.github.Blatzar:NiceHttp:+'
```

### Scraping a document with Nice Http

```kotlin
suspend fun main() {
    coroutineScope {
        val requests = Requests(okHttpClient, responseParser = parser)
        val jsonString = requests.get("https://swapi.dev/api/planets/1/").parsed<ParsedData>()
        println(jsonString.created)

    }
}

```