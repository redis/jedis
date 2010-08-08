# Jedis

Jedis is a blazingly small and sane redis java client.

Jedis was conceived to be EASY to use and FULLY COMPATIBLE with the latest version of redis.

Jedis is now fully compatible with the latest version of redis.

## What will be available soon?
- Sharding
- Connection pooling
- More and more code and performance improvements

But stay close because things are going fast and all this will be implemented soon!

## Ok.. so what can I do with Jedis?
All of the following reids features are supported:

- Sorting
- Connection handling
- Commands operating on all the kind of values
- Commands operating on string values
- Commands operating on hashes
- Commands operating on lists
- Commands operating on sets
- Commands operating on sorted sets
- Transactions
- Pipelining
- Publish/Subscribe
- Persistence control commands
- Remote server control commands

## How do I use it?

You can download the latests build at: 
    http://github.com/xetorthio/jedis/downloads

To use it just:
    
    Jedis jedis = new Jedis("localhost");
    jedis.connect();
    jedis.set("foo", "bar");
    String value = jedis.get("foo");

And you are done!

License
-------

Copyright (c) 2010 Jonathan Leibiusky

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

