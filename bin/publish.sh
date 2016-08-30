#!/bin/bash
set -e

version=$(sed -n -e 's/.*val nightly = "\(.*\)"/\1/p' src/main/scala/com/geirsson/codegen/Versions.scala)
tag="v${version}"
user="olafurpg"
repo="scala-db-codegen"
project_name="$repo"
tarfile="target/$repo.tar.gz"
project_url="https://github.com/$user/$repo"
current_branch=$(git rev-parse --abbrev-ref HEAD)

function assert-installed() {
  binary=$1
  command -v ${binary} >/dev/null 2>&1 || { echo >&2 "Missing dependency ${binary}, exiting."; exit 1; }
}

function assert-dependencies-are-installed() {
  assert-installed sbt
  assert-installed github-release
  assert-installed sbt
  assert-installed shasum
  assert-installed tar
}

function assert-preconditions() {
    if [ "$current_branch" != "master" ]; then
      echo "On branch $current_branch! You should be on master branch."
      exit 1
    fi
    assert-dependencies-are-installed
}

function confirm-release() {
    read -p "Release ${tag}? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]
    then
        exit 1
    fi
}

function assemble-jar() {
    sbt pack
}

function push-tag() {
    git tag ${tag} -m "See changelog."
    git push --tags
}

function maven-publish() {
    sbt publishSigned sonatypeRelease
}


function update-github-release() {
    rm -f ${tarfile}
    tar -cvzf ${tarfile} target/pack

    echo "Creating github release..."
    github-release release \
        --user $user \
        --repo $repo \
        --tag ${tag} \
        --name "${tag}" \
        --description "Changelog is here: $project_url"

    echo "Uploading tar..."
    github-release upload \
        --user $user \
        --repo $repo \
        --tag ${tag} \
        --name "$project_name.tar.gz" \
        --file ${tarfile}
}

assert-preconditions
confirm-release
assemble-jar
maven-publish
push-tag
update-github-release
echo "Released ${tag}!"
