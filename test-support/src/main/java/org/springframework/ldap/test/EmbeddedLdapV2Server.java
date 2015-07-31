/*
 * Copyright 2005-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ldap.test;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;

import java.io.File;

/**
 * Helper class for embedded Apache Directory Server.
 *
 * @author Edd� Mel�ndez
 * @since 2.0.4
 */
public final class EmbeddedLdapV2Server {
    private final DirectoryService directoryService;
    private final LdapServer ldapServer;
    private static File workingDirectory;

    private EmbeddedLdapServer(DirectoryService directoryService,
                               LdapServer ldapServer) {
        this.directoryService = directoryService;
        this.ldapServer = ldapServer;
    }

    public static EmbeddedLdapServer newEmbeddedServer(String defaultPartitionName, String defaultPartitionSuffix, int port)
            throws Exception{
        workingDirectory = new File(System.getProperty("java.io.tmpdir") + "/apacheds-test1");
        FileUtils.deleteDirectory(workingDirectory);

        DirectoryService directoryService = new DefaultDirectoryService();
        directoryService.setShutdownHookEnabled(true);
        directoryService.setAllowAnonymousAccess(true);

        directoryService.getChangeLog().setEnabled( false );

        JdbmPartition partition = new JdbmPartition(directoryService.getSchemaManager(), directoryService.getDnFactory());
        partition.setId(defaultPartitionName);
        partition.setSuffixDn(new Dn(defaultPartitionSuffix));
        directoryService.addPartition(partition);

        directoryService.startup();

        // Inject the apache root entry if it does not already exist
        if ( !directoryService.getAdminSession().exists( partition.getSuffixDn() ) )
        {
            Entry entry = directoryService.newEntry(new Dn(defaultPartitionSuffix));
            entry.add("objectClass", "top", "domain", "extensibleObject");
            entry.add("dc", defaultPartitionName);
            directoryService.getAdminSession().add( entry );
        }

        LdapServer ldapServer = new LdapServer();
        ldapServer.setDirectoryService(directoryService);

        TcpTransport ldapTransport = new TcpTransport(port);
        ldapServer.setTransports( ldapTransport );
        ldapServer.start();

        return new EmbeddedLdapServer(directoryService, ldapServer);
    }

    public void shutdown() throws Exception {
        ldapServer.stop();
        directoryService.shutdown();

        FileUtils.deleteDirectory(workingDirectory);
    }
}
