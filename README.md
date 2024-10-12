### Setup

In build.gradle repositories:

```groovy
maven { url 'https://jitpack.io' }
```

In app/build.gradle dependencies:

```groovy
implementation 'com.github.Blatzar:NiceHttp:0.4.4'
```

## Video tutorial 
https://t.me/azamovme/124

### Scraping a document with Nice Http

```kotlin
suspend fun main() {
    val scannerForNext = Scanner(System.`in`)
    coroutineScope {
        val uzmoviBase = UzmoviBase()
        print("Enter Movie Name :")
        val movieName = scanner.nextLine()
        displayLoadingAnimation("Searching for movies", Color.GREEN)
        val list = uzmoviBase.searchMovie(movieName)
        printlnColored(" Selected Movie: ${list[0].title}", Color.GREEN)
        displayLoadingAnimation("Loading Episodes", Color.GREEN)
        uzmoviBase.movieDetails(list[0]) // Get Movie Details  Scraping by href

    }
}

```

## _Thanks For_  [Blatzar](https://github.com/Blatzar)
