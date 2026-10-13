package com.docforge.app

import com.docforge.core.domain.io.ActiveTempFileRegistry
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ActiveTempFileRegistryAliasTest {
    @get:Rule val folder = TemporaryFolder()
    private val nowMillis = 1_800_000_000_000L
    private val minimumAgeMillis = 30L * 60L * 1000L

    private fun staleFileAndAlias(): Pair<File, File> {
        val aliasDir = folder.newFolder("alias")
        val actual = folder.newFile("docforge_alias.tmp")
        actual.writeText("still in use")
        assertTrue(actual.setLastModified(nowMillis - minimumAgeMillis * 2L))
        val alias = File(aliasDir, "../${actual.name}")
        assertTrue(alias.exists())
        return actual to alias
    }

    @Test fun aliasedOwnerProtectsStaleFileFromDirectCleanup() {
        val (actual, alias) = staleFileAndAlias()
        ActiveTempFileRegistry.register(alias)
        try {
            assertTrue(ActiveTempFileRegistry.isActive(actual))
            DocForgeTempFileCleanup.clean(folder.root, nowMillis, minimumAgeMillis)
            assertTrue(actual.exists())
        } finally {
            ActiveTempFileRegistry.unregister(alias)
        }
        DocForgeTempFileCleanup.clean(folder.root, nowMillis, minimumAgeMillis)
        assertFalse(actual.exists())
    }

    @Test fun mixedAliasOwnersProtectUntilLastRelease() {
        val (actual, alias) = staleFileAndAlias()
        ActiveTempFileRegistry.register(alias)
        ActiveTempFileRegistry.register(actual)
        try {
            ActiveTempFileRegistry.unregister(actual)
            assertTrue(ActiveTempFileRegistry.isActive(actual))
            DocForgeTempFileCleanup.clean(folder.root, nowMillis, minimumAgeMillis)
            assertTrue(actual.exists())
        } finally {
            ActiveTempFileRegistry.unregister(alias)
        }
        assertFalse(ActiveTempFileRegistry.isActive(actual))
        DocForgeTempFileCleanup.clean(folder.root, nowMillis, minimumAgeMillis)
        assertFalse(actual.exists())
    }

    @Test fun unmatchedEquivalentAliasCannotReleaseAnotherOwner() {
        val (actual, alias) = staleFileAndAlias()
        ActiveTempFileRegistry.register(alias)
        try {
            // Canonical identity is shared for protection, but ownership is not.
            ActiveTempFileRegistry.unregister(actual)
            assertTrue(ActiveTempFileRegistry.isActive(actual))
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(actual))
            DocForgeTempFileCleanup.clean(folder.root, nowMillis, minimumAgeMillis)
            assertTrue(actual.exists())
        } finally {
            ActiveTempFileRegistry.unregister(alias)
        }
        assertFalse(ActiveTempFileRegistry.isActive(actual))
    }

    @Test fun unmatchedLexicalAliasCannotReleaseDirectOwner() {
        val (actual, alias) = staleFileAndAlias()
        ActiveTempFileRegistry.register(actual)
        try {
            ActiveTempFileRegistry.unregister(alias)
            assertTrue(ActiveTempFileRegistry.isActive(actual))
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(alias))
            assertTrue(actual.exists())
        } finally {
            ActiveTempFileRegistry.unregister(actual)
        }
        assertFalse(ActiveTempFileRegistry.isActive(actual))
    }

    @Test fun directRegistrationBlocksAliasedDelete() {
        val (actual, alias) = staleFileAndAlias()
        ActiveTempFileRegistry.register(actual)
        try {
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(alias))
            assertTrue(actual.exists())
        } finally {
            ActiveTempFileRegistry.unregister(actual)
        }
        assertTrue(ActiveTempFileRegistry.deleteIfInactive(alias))
        assertFalse(actual.exists())
    }
    @Test fun symbolicLinkRegistrationProtectsOriginalTarget() {
        val original = folder.newFile("docforge_link_target.tmp")
        val link = File(folder.root, "alias_link.tmp")
        Files.createSymbolicLink(link.toPath(), original.toPath())
        ActiveTempFileRegistry.register(link)
        try {
            assertTrue(ActiveTempFileRegistry.isActive(original))
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(original))
            assertTrue(original.exists())
        } finally {
            ActiveTempFileRegistry.unregister(link)
        }
        assertFalse(ActiveTempFileRegistry.isActive(original))
    }

    @Test fun retargetedSymbolicLinkUnregisterReleasesOriginalIdentity() {
        val original = folder.newFile("docforge_first_target.tmp")
        val second = folder.newFile("docforge_second_target.tmp")
        val link = File(folder.root, "alias_link.tmp")
        Files.createSymbolicLink(link.toPath(), original.toPath())
        ActiveTempFileRegistry.register(link)
        try {
            Files.delete(link.toPath())
            Files.createSymbolicLink(link.toPath(), second.toPath())
            assertTrue(ActiveTempFileRegistry.isActive(original))
            assertFalse(ActiveTempFileRegistry.isActive(second))
        } finally {
            ActiveTempFileRegistry.unregister(link)
        }
        assertFalse(ActiveTempFileRegistry.isActive(original))
        assertFalse(ActiveTempFileRegistry.isActive(second))
        assertTrue(original.exists())
        assertTrue(second.exists())
    }

    @Test fun retargetedAliasWithTwoRegistrationsReleasesEachOriginalKey() {
        val first = folder.newFile("docforge_first_owner.tmp")
        val second = folder.newFile("docforge_second_owner.tmp")
        val link = File(folder.root, "alias_link.tmp")
        Files.createSymbolicLink(link.toPath(), first.toPath())
        ActiveTempFileRegistry.register(link)
        try {
            Files.delete(link.toPath())
            Files.createSymbolicLink(link.toPath(), second.toPath())
            ActiveTempFileRegistry.register(link)
            assertTrue(ActiveTempFileRegistry.isActive(first))
            assertTrue(ActiveTempFileRegistry.isActive(second))
            ActiveTempFileRegistry.unregister(link)
            assertTrue(ActiveTempFileRegistry.isActive(first))
            assertTrue(ActiveTempFileRegistry.isActive(second))
        } finally {
            ActiveTempFileRegistry.unregister(link)
        }
        assertFalse(ActiveTempFileRegistry.isActive(first))
        assertFalse(ActiveTempFileRegistry.isActive(second))
    }
    @Test fun retargetedAliasReleasedInRegistrationOrderNeverUnprotectsOtherOwner() {
        val first = folder.newFile("docforge_fifo_first.tmp")
        val second = folder.newFile("docforge_fifo_second.tmp")
        val link = File(folder.root, "alias_link.tmp")
        Files.createSymbolicLink(link.toPath(), first.toPath())
        ActiveTempFileRegistry.register(link)
        Files.delete(link.toPath())
        Files.createSymbolicLink(link.toPath(), second.toPath())
        ActiveTempFileRegistry.register(link)
        try {
            // First owner exits before second. The API has no owner token,
            // so both identities must stay protected until final release.
            ActiveTempFileRegistry.unregister(link)
            assertTrue(ActiveTempFileRegistry.isActive(first))
            assertTrue(ActiveTempFileRegistry.isActive(second))
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(second))
        } finally {
            ActiveTempFileRegistry.unregister(link)
        }
        assertFalse(ActiveTempFileRegistry.isActive(first))
        assertFalse(ActiveTempFileRegistry.isActive(second))
        assertTrue(first.exists())
        assertTrue(second.exists())
    }

    @Test fun danglingSymbolicLinkProtectsTargetCreatedAfterRegistration() {
        val target = File(folder.root, "docforge_late_target.tmp")
        val link = File(folder.root, "alias_late.tmp")
        Files.createSymbolicLink(link.toPath(), target.toPath())
        ActiveTempFileRegistry.register(link)
        try {
            target.writeText("created later")
            assertTrue(ActiveTempFileRegistry.isActive(target))
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(target))
            assertTrue(target.exists())
        } finally {
            ActiveTempFileRegistry.unregister(link)
        }
        assertFalse(ActiveTempFileRegistry.isActive(target))
    }

    @Test fun danglingSymbolicLinkChainProtectsLaterTarget() {
        val target = File(folder.root, "docforge_chain_target.tmp")
        val middle = File(folder.root, "alias_middle.tmp")
        val outer = File(folder.root, "alias_outer.tmp")
        Files.createSymbolicLink(middle.toPath(), target.toPath())
        Files.createSymbolicLink(outer.toPath(), middle.toPath())
        ActiveTempFileRegistry.register(outer)
        try {
            target.writeText("created later")
            assertTrue(ActiveTempFileRegistry.isActive(target))
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(target))
            assertTrue(target.exists())
        } finally {
            ActiveTempFileRegistry.unregister(outer)
        }
        assertFalse(ActiveTempFileRegistry.isActive(target))
    }

    @Test fun parentSymlinkDotDotDoesNotProtectWrongFile() {
        val base = folder.newFolder("base")
        val external = folder.newFolder("external")
        val nested = File(external, "nested").apply { assertTrue(mkdir()) }
        val actual = File(external, "docforge_parent_target.tmp").apply { writeText("in use") }
        val unrelated = File(base, actual.name).apply { writeText("other") }
        val link = File(base, "link")
        Files.createSymbolicLink(link.toPath(), nested.toPath())
        val alias = File(link, "../${actual.name}")
        assertTrue(alias.canonicalFile == actual.canonicalFile)
        ActiveTempFileRegistry.register(alias)
        try {
            assertTrue(ActiveTempFileRegistry.isActive(actual))
            assertFalse(ActiveTempFileRegistry.isActive(unrelated))
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(actual))
            assertTrue(actual.exists())
        } finally {
            ActiveTempFileRegistry.unregister(alias)
        }
        assertFalse(ActiveTempFileRegistry.isActive(actual))
    }

    @Test fun danglingParentSymlinkProtectsTargetCreatedLater() {
        val targetDirectory = File(folder.root, "late_directory")
        val parentLink = File(folder.root, "parent_link")
        Files.createSymbolicLink(parentLink.toPath(), targetDirectory.toPath())
        val alias = File(parentLink, "docforge_late_parent.tmp")
        ActiveTempFileRegistry.register(alias)
        try {
            assertTrue(targetDirectory.mkdir())
            val actual = File(targetDirectory, "docforge_late_parent.tmp")
            actual.writeText("active")
            assertTrue(ActiveTempFileRegistry.isActive(actual))
            assertFalse(ActiveTempFileRegistry.deleteIfInactive(actual))
            assertTrue(actual.exists())
        } finally {
            ActiveTempFileRegistry.unregister(alias)
        }
        assertFalse(ActiveTempFileRegistry.isActive(File(targetDirectory, "docforge_late_parent.tmp")))
    }

}
