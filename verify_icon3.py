import pefile
import struct

# Check the intermediate file (modified launcher only)
pe = pefile.PE('/tmp/HMCLauncher_new.exe')

for entry in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if entry.id == pefile.RESOURCE_TYPE['RT_GROUP_ICON']:
        for sub in entry.directory.entries:
            for lang in sub.directory.entries:
                rva = lang.data.struct.OffsetToData
                size = lang.data.struct.Size
                data = pe.get_data(rva, size)
                print(f'Intermediate RT_GROUP_ICON: RVA={hex(rva)}, size={size}')
                print(f'  Raw hex: {data.hex()}')
                if size >= 6:
                    count = struct.unpack_from('<H', data, 4)[0]
                    print(f'  icon count: {count}')
                    for i in range(count):
                        off = 6 + i*14
                        if off + 14 <= len(data):
                            w = data[off]; h = data[off+1]
                            sz = struct.unpack_from('<I', data, off+8)[0]
                            print(f'  Icon {i}: {w}x{h}, data_size={sz}')
                            # Check if RT_ICON data at this index matches
                            icon_idx = 0
                            for e2 in pe.DIRECTORY_ENTRY_RESOURCE.entries:
                                if e2.id == pefile.RESOURCE_TYPE['RT_ICON']:
                                    for s2 in e2.directory.entries:
                                        if icon_idx == i:
                                            for l2 in s2.directory.entries:
                                                rva2 = l2.data.struct.OffsetToData
                                                sz2 = l2.data.struct.Size
                                                print(f'    RT_ICON[{i}]: RVA={hex(rva2)}, size={sz2}')
                                        icon_idx += 1