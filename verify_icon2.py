import pefile
import struct

pe = pefile.PE('JMCL/build/libs/JVM-MCL-DEV2026.2.1.exe')

# Check RT_ICON entries directly
for entry in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if entry.id == pefile.RESOURCE_TYPE['RT_ICON']:
        for sub in entry.directory.entries:
            for lang in sub.directory.entries:
                rva = lang.data.struct.OffsetToData
                size = lang.data.struct.Size
                data = pe.get_data(rva, min(size, 64))
                print(f'RT_ICON: RVA={hex(rva)}, size={size}')
                print(f'  First bytes: {data[:16].hex()}')

# Check RT_GROUP_ICON entry directly
for entry in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if entry.id == pefile.RESOURCE_TYPE['RT_GROUP_ICON']:
        print(f'\nRT_GROUP_ICON:')
        for sub in entry.directory.entries:
            for lang in sub.directory.entries:
                rva = lang.data.struct.OffsetToData
                size = lang.data.struct.Size
                data = pe.get_data(rva, size)
                print(f'  RVA={hex(rva)}, size={size}')
                if size >= 6:
                    count = struct.unpack_from('<H', data, 4)[0]
                    print(f'  icon count: {count}')
                    for i in range(count):
                        off = 6 + i*14
                        if off + 14 <= len(data):
                            w = data[off]
                            h = data[off+1]
                            sz = struct.unpack_from('<I', data, off+8)[0]
                            print(f'  Icon {i}: {w}x{h}, data_size={sz}')