import pefile, struct
pe = pefile.PE('JMCL/build/libs/JVM-MCL-DEV2026.2.1.exe')
for rsrc in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if rsrc.id == pefile.RESOURCE_TYPE['RT_GROUP_ICON']:
        for sub in rsrc.directory.entries:
            for lang in sub.directory.entries:
                data = pe.get_data(lang.data.struct.OffsetToData, lang.data.struct.Size)
                count = struct.unpack_from('<H', data, 4)[0]
                print(f'EXE icons ({count} total):')
                for i in range(count):
                    off = 6 + i * 14
                    w = data[off]; h = data[off + 1]
                    bpp = struct.unpack_from('<H', data, off + 6)[0]
                    sz = struct.unpack_from('<I', data, off + 8)[0]
                    rid = struct.unpack_from('<H', data, off + 12)[0]
                    print(f'  Icon {i}: {w}x{h}, {bpp}bpp, {sz} bytes, ID={rid}')
print('Verification complete!')