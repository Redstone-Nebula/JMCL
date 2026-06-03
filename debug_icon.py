import struct

# Check the intermediate file directly
with open('/tmp/HMCLauncher_clean_new.exe', 'rb') as f:
    data = f.read()

# The RT_GROUP_ICON was written at offset 0x6c5a0, size=62
off = 0x6c5a0
raw = data[off:off+62]
print(f'Raw hex at 0x{off:x}: {raw.hex()}')

count = struct.unpack_from('<H', raw, 4)[0]
print(f'Icon count: {count}')
for i in range(count):
    eo = 6 + i * 14
    if eo + 14 <= len(raw):
        w = raw[eo]; h = raw[eo+1]
        planes = struct.unpack_from('<H', raw, eo+4)[0]
        bpp = struct.unpack_from('<H', raw, eo+6)[0]
        sz = struct.unpack_from('<I', raw, eo+8)[0]
        rid = struct.unpack_from('<H', raw, eo+12)[0]
        print(f'  Icon {i}: {w}x{h}, planes={planes}, {bpp}bpp, {sz} bytes, ID={rid}')

# Also check what pefile sees
import pefile
pe = pefile.PE('/tmp/HMCLauncher_clean_new.exe')
for rsrc in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if rsrc.id == pefile.RESOURCE_TYPE['RT_GROUP_ICON']:
        for sub in rsrc.directory.entries:
            for lang in sub.directory.entries:
                rva = lang.data.struct.OffsetToData
                size = lang.data.struct.Size
                offset = pe.get_offset_from_rva(rva)
                print(f'\npefile reads RT_GROUP_ICON: RVA=0x{rva:x}, file_offset=0x{offset:x}, size={size}')
                data2 = pe.get_data(rva, size)
                print(f'  Raw hex: {data2.hex()}')
                if data2 != raw[:size]:
                    print('  *** MISMATCH with direct read! ***')