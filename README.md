# Jedis

Jedis is a blazingly small and sane redis java client.

Jedis was conceived to be EASY to use and FULLY COMPATIBLE with the latest version of redis.

Jedis is a WORK IS PROGRESS.

## What's still missing?
Right now almost everything. You can just PING, GET and SET. But stay close because whenever I have a couple of free hours it will support almost everything!

## How do I use it?

You can download the latests build at: 
    http://github.com/downloads/xetorthio/jedis/jedis-0.0.1-SNAPSHOT.jar

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

