# Jedis Documentation

This documentation uses [MkDocs](https://www.mkdocs.org/) to generate the static site.

See [mkdocs.yml](../mkdocs.yml) for the configuration. 

To develop the documentation locally, you can use the included [Dockerfile](Dockerfile) to build a container with all the 
dependencies, and run it to preview your changes:

```bash
# in docs/
docker build -t squidfunk/mkdocs-material .
# cd ..
docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material 
```