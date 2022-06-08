# Profile-Store

This project makes use of [quick](https://d9p.io/) to implement the aggregation of
a stream of *listening events* into easily queryable user profiles.
Additionally, it demonstrates how to include a rest endpoint in the query.

For the demo, we use a sample of the listening event corpus [LFM-1b](http://www.cp.jku.at/datasets/LFM-1b/)

It can be deployed using the scripts in profile-store/deployment (see Readme there).

### Query

The quick gateway can be queried using the schema in profile-store/deployment/schema.gql:

```
type Query {
    getUserProfile(userId: Long!): UserProfile
    getArtistRecommendations(
        userId: Long!,
        field: FieldType!=ARTIST,
        limit: Int,
        walks: Int,
        walkLength: Int,
        resetProbability: Float
    ):  Recommendations @rest(url: "http://recommender/recommendation",
        pathParameter: ["userId", "field"],
        queryParameter: ["limit", "walks", "walkLength", "resetProbability"])
}
```

where the structure of a user profile is as follows
```
type UserProfile {
    totalListenCount: Long! @topic(name: "counts", keyArgument: "userId")
    firstListenEvent: Long! @topic(name: "firstlisten", keyArgument: "userId")
    lastListenEvent: Long! @topic(name: "lastlisten", keyArgument: "userId")
    artistCharts: NamedArtistCharts! @topic(name: "topartists", keyArgument: "userId")
    albumCharts: NamedAlbumCharts! @topic(name: "topalbums", keyArgument: "userId")
    trackCharts: NamedTrackCharts! @topic(name: "toptracks", keyArgument: "userId")
}
```

Each field is calculated by an independent app in the quick environment.
The apps for the UserProfile are based on the sub-packages user-listen-count, user-listen-activity and user-charts. 

### Demo data

We use a small sample from the LFM-1b data set, which can be found in profile-store/data. The directory also includes
a sampler to generate new data from the huge original set. The base data, that resolve the ids for artists, albums and tracks, and also the demo listening data can be supplied with a helm deployment.
For the demo listening data it is possible to define an emission frequency.
The corresponding producers are found in sub-packages fieldtables and listeningevents.
Alternatively, the data can be passed through the quick ingest.
