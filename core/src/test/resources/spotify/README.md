# Spotify's replies

These are real replies of Spotify's Web API, fetched on 2 October 2026 with a client-credentials token
(a key in development mode) and `market=DE`. They are saved as they came, without the request: no key and
no token is in them.

- `search-track.json`: `search?q=bohemian rhapsody&type=track`
- `search-track-by.json`: `search?q=track:"komet" artist:"udo lindenberg"&type=track`
- `search-track-none.json`: a search that found nothing
- `search-album.json`: `search?q=discovery daft punk&type=album`
- `album-tracks.json`: `albums/{id}/tracks?limit=1&market=DE` for that album
- `search-artist.json`: `search?q=daft punk&type=artist`
- `search-artist-tracks.json`: `search?q=artist:"Daft Punk"&type=track`. Its one song is somebody else's, on
  which they are guests.
- `search-playlist.json`: `search?q=deep focus&type=playlist`. Spotify's own playlists come back as `null`
  for a key of this kind.
- `artist-top-tracks-403.json`: `artists/{id}/top-tracks`, which a key of this kind may not ask for (HTTP 403)
- `error-401.json`: a request with a token that is not one (HTTP 401)
- `token-invalid-client.json`: the token address, asked with a wrong key (HTTP 400)

One file is **written by hand**: `token.json`, in the shape of the token address's reply. A real one is a
token, and is never saved.

The one change made to them: the playlist's owner in `search-playlist.json` (a real account's name and id) is replaced by a made-up one.
