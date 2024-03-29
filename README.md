# Book Web

This is a practice application to help me explore some of the webapp
techniques I've been interested in for a while, the main one of which
is how to use a bookmarklet.

## What the App Does

Provides a simple bookmarking service. You join, sign-in, put a
bookmarklet on your browser's bookmark bar, surf, surf, surf, then hit
the bookmarklet when you want to bookmark a page. I imagine this is
exactly like countless other very similar services. The difference?
_I_ wrote this one and expect to learn quite a bit from it.

You can read all you want about how things are done, and then you can
do them. Two different things.

As it turns out, I think I have an actual use for this: I use a Mac at
work, and a Mac at home, and I've synched bookmarks and reading-list
links via iCloud. The links show up on my phone and iPad. Works great.

But I want to keep a clean separation between my work Mac and my home
Mac. If I find something of interest at work, where do I put that link
so I can get back to it at home? Could email it. Could use one of the
admirable third party services out there. Or I could write my own
solution.

Which is what this project aims to do.

## Goals, To Do, Loose Ends

What I need to implement before I consider this application done.

  * ~~Basic bookmarklet~~

  * ~~Server side app, Clojure + Mongo~~

  * ~~Authentication via persistent, encrypted cookie~~

  * ~~Search bookmarks~~

  * ~~Add bookmark~~

  * ~~Proper keyboard intercepts (return, esc) for bookmarklet~~

  * ~~Change your password/email stuff~~

  * ~~Cool, no-fuss way to join website (capcha begone!)~~

  * ~~Display bookmarks according to logged in user~~

  * ~~Delete bookmark~~

  * ~~Edit bookmark~~

  * ~~Bookmark page should continuously update so you can see new
    bookmarks appear~~

  * ~~BUG: if user changes email address, need to change it across
    bookmarks list.~~

  * ~~Proper bookmarklet handling when requests are denied~~

  * ~~Rename the app as "linx"~~

  * ~~Create a generic key/value collection in Mongo for storing
    encryption keys, config stuff, stats, anything, really.~~

  * ~~Store crypto key in mongo (thus the generic key/value store)~~

  * Proper file based configuration (just read in a JSON structure).

  * Make a silent movie demonstrating the app

  * Tidy up the server side code (but be sloppy until then).

I'll add more to the list as I figure out stuff I forgot, but "tidy up
the code" will always be last.

## Maintenance Issues

Stuff I don't have to finish to be done, but ought to finish over
time.

  * Check proper CSS interference when the bookmark form is displayed
    on other pages. (How to I properly reset everything so the style's
    my own and doesn't interfere with host pages?) I can keep working
    on this as I encounter more and more pages.

  * Bullet-proofing form entries.

  * Sensible account change. Should be one form to change email
    address, another for password. It's kind of UX ugly to do both
    with the same form. But it works.

  * Proper error reporting when submitting a form results in a
    non-good server response.

## Technologies

Using the following technologies:

  * Mongo DB

  * Clojure
    - compojure
    - monger
    - hiccup
    - assorted other convenience libs (json, logging, digest, crypto)

  * Javascript
    - jQuery

Nothing too fancy. I picked these because they can get me up and
prototyping about as quickly as possible, and because they use
microframeworks, which I favor because they allow the problem I need
to solve to dictate the shape the the solution, rather than the
reverse.

## License

Copyright © 2012 Zentrope

Distributed under the Eclipse Public License, the same as Clojure.
