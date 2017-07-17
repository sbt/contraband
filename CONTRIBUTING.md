
### Deployment steps

```
$ jenv shell 1.6
$ sbt
> clean
> publishSigned
```

```
$ jenv shell 1.8
$ sbt
> ++2.12.2
> ^^1.0.0-RC2
> clean
> publishSigned
```
