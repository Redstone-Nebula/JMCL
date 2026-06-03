import pefile, struct

pe = pefile.PE('/tmp/HMCLauncher_clean.exe')

for rsrc in pe.DIRECTORY_ENTRY_RESOURCE.entries:
    if rsrc.id == pefile.RESOURCE_TYPE['RT_VERSION']:
        for sub in rsrc.directory.entries:
            for lang in sub.directory.entries:
                rva = lang.data.struct.OffsetToData
                size = lang.data.struct.Size
                data = pe.get_data(rva, size)
                print(f'RT_VERSION: rva=0x{rva:x}, size={size}')
                print(f'First 300 bytes:')
                for i in range(0, min(300, len(data)), 32):
                    hex_str = ' '.join(f'{b:02x}' for b in data[i:i+32])
                    ascii_str = ''.join(chr(b) if 32 <= b < 127 else '.' for b in data[i:i+32])
                    print(f'  {i:04x}: {hex_str}  {ascii_str}')
                print(f'\nLast 300 bytes:')
                for i in range(max(0, len(data)-300), len(data), 32):
                    hex_str = ' '.join(f'{b:02x}' for b in data[i:i+32])
                    ascii_str = ''.join(chr(b) if 32 <= b < 127 else '.' for b in data[i:i+32])
                    print(f'  {i:04x}: {hex_str}  {ascii_str}')