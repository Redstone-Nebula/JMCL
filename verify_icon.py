import pefile
pe = pefile.PE('JMCL/build/libs/JVM-MCL-DEV2026.2.1.exe')
for entry in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if entry.id == pefile.RESOURCE_TYPE['RT_GROUP_ICON']:
        for sub in entry.directory.entries:
            for lang in sub.directory.entries:
                data = pe.get_data(lang.data.struct.OffsetToData, lang.data.struct.Size)
                count = int.from_bytes(data[4:6], 'little')
                print(f'EXE icons ({count} total):')
                for i in range(count):
                    off = 6 + i*14
                    w = data[off]
                    h = data[off+1]
                    bpp = int.from_bytes(data[off+6:off+8], 'little')
                    sz = int.from_bytes(data[off+8:off+12], 'little')
                    print(f'  Icon {i}: {w}x{h}, {bpp}bpp, {sz} bytes')
                print('Icon replaced successfully!')