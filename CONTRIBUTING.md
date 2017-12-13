
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
> ++2.12.4
> ^^1.0.4
> clean
> publishSigned
```
