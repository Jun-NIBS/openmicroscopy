#!/usr/bin/env python
# encoding: utf-8

"""
Retrieves the various client builds for the various platforms and creates
composite builds.
"""

# Copyright (C) 2009 University of Dundee

# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.

# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.


import urllib
import sys
import re
import os

from subprocess import Popen
from getopt import getopt, GetoptError
from glob import glob
from zipfile import ZipFile

# Handle Python 2.5 built-in ElementTree
try:
    from xml.etree.ElementTree import XML, ElementTree, tostring
except ImportError:
    from elementtree.ElementTree import XML, ElementTree, tostring


HUDSON_ROOT = 'http://hudson.openmicroscopy.org.uk/job'

HUDSON_XML_SUFFIX = 'lastBuild/api/xml'

INSIGHT_JOB_NAME = 'INSIGHT-Beta4.2'

IMPORTER_JOB_NAME = 'OMERO-Beta4.2'

TARGET_PREFIX = 'OMERO.clients-Beta4.2.0'

if "BUILD_NUMBER" in os.environ:
    TARGET_PREFIX = '%s-b%s' % (TARGET_PREFIX, os.environ["BUILD_NUMBER"])


def download(job, regex):
    """Grabs platform specific distribution targets from Hudson"""
    url = urllib.urlopen("/".join([HUDSON_ROOT, job, HUDSON_XML_SUFFIX]))
    hudson_xml = url.read()
    url.close()
    root = XML(hudson_xml)

    building = root.findtext("./building")
    if building == 'true':
        print '%s build in progress, exiting...' % job
        sys.exit(1)
    revision = root.findtext("./changeSet/revision/revision")
    artifacts = root.findall("./artifact")
    print "Retrieving %s job artifacts from revision: %s" % (job, revision)
    base_url = "/".join([HUDSON_ROOT, job, 'lastBuild/artifact'])
    new_artifacts = list()
    for artifact in artifacts:
        filename = artifact.findtext("fileName")
        if not regex.match(filename):
            continue
        artifact_url = "/".join([base_url, artifact.findtext("relativePath")])
        print "Downloading %s from URL %s" % (filename, artifact_url)
        urllib.urlretrieve(artifact_url , filename)
        new_artifacts.append(filename)
    return [revision, new_artifacts]

def extract(artifact, target, ignore):
    """
    Extracts all files of a given artifact to a target directory filtering and
    mangling paths as necessary.
    """
    zip_file = ZipFile(artifact, 'r')
    try:
        for zip_info in zip_file.infolist():
            name = zip_info.filename
            out_name = name[name.find('/') + 1:]
            if len(out_name) == 0 or os.path.split(name)[1] in ignore:
                continue
            out = None
            try:
                path = "/".join([target, out_name])
                if path[-1:] == '/':
                    if not os.path.exists(path):
                        os.makedirs(path)
                    continue
                print "Extracting: %s" % path
                out = open(path, 'w')
                out.write(zip_file.read(name))
                os.chmod(path, zip_info.external_attr >> 16L)
            finally:
                if out is not None:
                    out.close()
    finally:
        zip_file.close()

def compress(target, base):
    """Creates a ZIP recursively from a given base directory."""
    zip_file = ZipFile(target, 'w')
    try:
        for root, dirs, names in os.walk(base):
            for name in names:
                path = os.path.join(root, name)
                print "Compressing: %s" % path
                zip_file.write(path)
    finally:
        zip_file.close()
    

#
# Create the composite Windows client build
#
target_artifacts = list()
regex = re.compile(r'.*win.zip')
revision, artifacts = download(INSIGHT_JOB_NAME, regex)
target_artifacts += artifacts
revision, artifacts = download(IMPORTER_JOB_NAME, regex)
target_artifacts += artifacts
target = '%s.win' % TARGET_PREFIX
ignore = ['omero_client.jar', 'OmeroImporter-Beta-4.2.0-DEV.jar',
          'omero-clients-util-r6779-b12.jar']

for artifact in target_artifacts:
    extract(artifact, target, ignore)
compress('%s.zip' % target, target)

#
# Create the composite Mac OS X client build
#
target_artifacts = list()
revision, artifacts = download(INSIGHT_JOB_NAME, re.compile(r'.*OSX.zip'))
target_artifacts += artifacts
revision, artifacts = download(IMPORTER_JOB_NAME, re.compile(r'.*mac.zip'))
target_artifacts += artifacts
target = '%s.mac' % TARGET_PREFIX

for artifact in target_artifacts:
    extract(artifact, target, [])
compress('%s.zip' % target, target)

#
# Create the composite Linux client build
#
target_artifacts = list()
regex = re.compile(r'.*b\d+.zip')
revision, artifacts = download(INSIGHT_JOB_NAME, regex)
target_artifacts += artifacts
regex = re.compile(r'.*importer.*b\d+.zip')
revision, artifacts = download(IMPORTER_JOB_NAME, regex)
target_artifacts += artifacts
target = '%s.linux' % TARGET_PREFIX
# Since Insight relies on its MANIFEST to start via the JAR, we're leaving
# libs/OmeroImporter-Beta-4.1.0-DEV.jar in the ZIP.
ignore = ['omero_client.jar', 'omero-clients-util-r7223-b1483.jar']

for artifact in target_artifacts:
    extract(artifact, target, ignore)
# Again, because Insight relies on its MANIFEST we're going to rename the
# omero_client.jar to fit its requirements.
client_jar = glob(os.path.join(target, 'libs', 'omero_client*.jar'))[0]
os.rename(client_jar, os.path.join(target, 'libs', 'omero_client.jar'))
compress('%s.zip' % target, target)
