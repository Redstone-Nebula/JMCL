import struct, pefile

with open('JMCL/build/libs/JVM-MCL-DEV2026.2.1.exe', 'rb') as f:
    data = f.read()

pe = pefile.PE('JMCL/build/libs/JVM-MCL-DEV2026.2.1.exe')

for rsrc in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if rsrc.id == pefile.RESOURCE_TYPE['RT_GROUP_ICON']:
        for sub in rsrc.directory.entries:
            for lang in sub.directory.entries:
                rva = lang.data.struct.OffsetToData
                size = lang.data.struct.Size
                offset = pe.get_offset_from_rva(rva)
                raw_at_offset = data[offset:offset + size]
                print(f'Final EXE RT_GROUP_ICON: RVA=0x{rva:x}, offset=0x{offset:x}, size={size}')
                print(f'  Hex: {raw_at_offset.hex()}')
                count = struct.unpack_from('<H', raw_at_offset, 4)[0]
                print(f'  Icon count: {count}')
                for i in range(count):
                    eo = 6 + i * 14
                    w = raw_at_offset[eo]; h = raw_at_offset[eo+1]
                    bpp = struct.unpack_from('<H', raw_at_offset, eo+6)[0]
                    sz = struct.unpack_from('<I', raw_at_offset, eo+8)[0]
                    rid = struct.unpack_from('<H', raw_at_offset, eo+12)[0]
                    print(f'  Icon {i}: {w}x{h}, {bpp}bpp, {sz} bytes, ID={rid}')