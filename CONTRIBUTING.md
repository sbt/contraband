
### Deployment steps

* bump `version` in `build.sbt` & write out the notes in `notes/`
* commit the changes: `git commit -am 'Prep for 0.3.3 release'`
* take the release: `git tag -a -s -m 'Version 0.3.3' v0.3.3`
* publish:

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
> ^^1.1.1
> clean
> publishSigned
```

* close the respository in https://oss.sonatype.org/#stagingRepositories
* release the repository in https://oss.sonatype.org/#stagingRepositories
* publish the package in https://bintray.com/sbt/sbt-plugin-releases/sbt-contraband
* push the commits & tag: `git push --follow-tags`
* make a GitHub release in https://github.com/sbt/contraband/releases
