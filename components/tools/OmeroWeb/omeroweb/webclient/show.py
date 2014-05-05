#!/usr/bin/env python
# -*- coding: utf-8 -*-

#
# Copyright (c) 2014 University of Dundee & Open Microscopy Environment.
# All rights reserved.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

"""
Generic functionality for handling particular links and "showing" objects
in the OMERO.web tree view.
"""

import re

from django.core.urlresolvers import reverse


class IncorrectMenuError(Exception):
    """Exception to signal that we are on the wrong menu."""

    def __init__(self, uri):
        """
        Constructs a new Exception instance.

        @param uri URI to redirect to.
        @type uri String
        """
        super(Exception, self).__init__()
        self.uri = uri


class Show(object):
    """
    This object is used by most of the top-level pages.  The "show" and
    "path" query strings are used by this object to both direct OMERO.web to
    the correct locations in the hierarchy and select the correct objects
    in that hierarchy.
    """

    # List of prefixes that are at the top level of the tree
    TOP_LEVEL_PREFIXES = ('project', 'screen')

    # List of supported object types
    SUPPORTED_OBJECT_TYPES = (
        'project', 'dataset', 'image', 'screen', 'plate', 'tag',
        'acquisition', 'run', 'well'
    )

    # Regular expression which declares the format for a "path" used either
    # in the "path" or "show" query string.  No modifications should be made
    # to this regex without corresponding unit tests in
    # "tests/unit/test_show.py".
    PATH_REGEX = re.compile(
        r'^(?P<object_type>\w+)\.?(?P<key>\w+)?[-=](?P<value>.*)'
    )

    def __init__(self, conn, request, menu):
        """
        Constructs a Show instance.  The instance will not be fully
        initialised until the first retrieval of the L{Show.first_selected}
        property.

        @param conn OMERO gateway.
        @type conn L{omero.gateway.BlitzGateway}
        @param request Django HTTP request.
        @type request L{django.http.HttpRequest}
        @param menu Literal representing the current menu we are on.
        @type menu String
        """
        # The list of "paths" ("type-id") we have been requested to
        # show/select in the user interface.  May be modified if one or
        # more of the elements is not in the tree.  This is currently the
        # case for all Screen-Plate-Well hierarchy elements below Plate
        # (Well for example).
        self._initially_select = list()
        # The nodes of the tree that will be initially open based on the
        # nodes that are initially selected.
        self._initially_open = None
        # The owner of the node closest to the root of the tree from the
        # list of initially open nodes.
        self._initially_open_owner = None
        # First selected node from the requested initially open "paths"
        # that is first loaded on first retrieval of the "first_selected"
        # property.
        self._first_selected = None

        self.conn = conn
        self.request = request
        self.menu = menu

        path = self.request.REQUEST.get('path', '').split('|')[-1]
        self._add_if_supported(path)

        show = self.request.REQUEST.get('show', '')
        for path in show.split('|'):
            self._add_if_supported(path)

    def _add_if_supported(self, path):
        """Adds a path to the initially selected list if it is supported."""
        m = self.PATH_REGEX.match(path)
        if m is None:
            return
        object_type = m.group('object_type')
        key = m.group('key')
        value = m.group('value')
        if key is None:
            key = 'id'
        if object_type in self.SUPPORTED_OBJECT_TYPES:
            # 'run' is an alternative for 'acquisition'
            object_type = object_type.replace('run', 'acquisition')
            self._initially_select.append(
                '%s.%s-%s' % (object_type, key, value)
            )

    def _load_first_selected(self, first_obj, attributes):
        """
        Loads the first selected object from the server.

        @param first_obj Type of the first selected object.
        @type first_obj String
        @param first_id ID of the first selected object.
        @type first_id Long
        """
        first_selected = None
        if first_obj == "tag":
            # Tags have an "Annotation" suffix added to the object name so
            # need to be loaded differently.
            first_selected = self.conn.getObject(
                "TagAnnotation", attributes=attributes
            )
        else:
            # All other objects can be loaded by prefix and id.
            first_selected = self.conn.getObject(
                first_obj, attributes=attributes
            )

        if first_obj == "well":
            # Wells aren't in the tree, so we need to look up the parent
            well_sample = first_selected.getWellSample()
            parent_node = None
            parent_type = None
            # It's possible that the Well that we've been requested to show
            # has no fields (WellSample instances).  In that case the Plate
            # will be used but we don't have much choice.
            if well_sample is not None:
                parent_node = well_sample.getPlateAcquisition()
                parent_type = "acquisition"
            if parent_node is None:
                # No PlateAcquisition for this well, use Plate instead
                parent_node = first_selected.getParent()
                parent_type = "plate"
            first_selected = parent_node
            self._initially_open = [
                "%s-%s" % (parent_type, parent_node.getId())
            ]
        else:
            self._initially_open = [
                '%s-%s' % (first_obj, first_selected.getId())
            ]
        self._initially_select = self._initially_open[:]
        self._initially_open_owner = first_selected.details.owner.id.val
        return first_selected

    def _find_first_selected(self):
        """Finds the first selected object."""
        if len(self._initially_select) == 0:
            return None

        # tree hierarchy open to first selected object
        self._initially_open = [self._initially_select[0]]
        m = self.PATH_REGEX.match(self._initially_open[0])
        if m is None:
            return None
        first_obj = m.group('object_type')
        # if we're showing a tag, make sure we're on the tags page...
        if first_obj == "tag" and self.menu != "usertags":
            raise IncorrectMenuError(
                reverse(viewname="load_template", args=['usertags']) +
                "?show=" + self._initially_select[0]
            )
        first_selected = None
        try:
            key = m.group('key')
            value = m.group('value')
            if key == 'id':
                value = long(value)
            attributes = {key: value}
            # Set context to 'cross-group'
            self.conn.SERVICE_OPTS.setOmeroGroup('-1')
            first_selected = self._load_first_selected(first_obj, attributes)
        except:
            pass
        if first_obj not in self.TOP_LEVEL_PREFIXES:
            # Need to see if first item has parents
            if first_selected is not None:
                for p in first_selected.getAncestry():
                    if first_obj == "tag":
                        # Parents of tags must be tags (no OMERO_CLASS)
                        self._initially_open.insert(0, "tag-%s" % p.getId())
                    else:
                        self._initially_open.insert(
                            0, "%s-%s" % (p.OMERO_CLASS.lower(), p.getId())
                        )
                        self._initially_open_owner = p.details.owner.id.val
                m = self.PATH_REGEX.match(self._initially_open[0])
                if m.group('object_type') == 'image':
                    self._initially_open.insert(0, "orphaned-0")
        return first_selected

    @property
    def first_selected(self):
        """
        Retrieves the first selected object.  The first time this method is
        invoked on the instance the actual retrieval is performed.  All other
        invocations retrieve the same instance without server interaction.
        """
        if self._first_selected is None:
            self._first_selected = self._find_first_selected()
        return self._first_selected

    @property
    def initially_select(self):
        """
        Retrieves the list of "paths" ("type-id") we have been requested to
        show/select in the user interface.  May be different than we were
        first initialised with due to certain nodes of the Screen-Plate-Well
        hierachy not being present in the tree.  Should not be invoked until
        after first retrieval of the L{Show.first_selected} property.
        """
        return self._initially_select

    @property
    def initially_open(self):
        """
        Retrieves the nodes of the tree that will be initially open based on
        the nodes that are initially selected.  Should not be invoked until
        after first retrieval of the L{Show.first_selected} property.
        """
        return self._initially_open

    @property
    def initially_open_owner(self):
        """
        Retrieves the owner of the node closest to the root of the tree from
        the list of initially open nodes.  Should not be invoked until
        after first retrieval of the L{Show.first_selected} property.
        """
        return self._initially_open_owner
